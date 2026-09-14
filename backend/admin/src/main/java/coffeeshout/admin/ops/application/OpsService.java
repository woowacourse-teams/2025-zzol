package coffeeshout.admin.ops.application;

import coffeeshout.admin.ops.domain.DeadLetter;
import coffeeshout.admin.ops.domain.DeadLetterSource;
import coffeeshout.admin.ops.domain.MigrationHistoryRepository;
import coffeeshout.admin.ops.domain.MigrationRecord;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.outbox.OutboxEvent;
import coffeeshout.global.outbox.OutboxEventRepository;
import coffeeshout.global.outbox.OutboxStatus;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterEntity;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterJpaRepository;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시스템 운영.
 *
 * <p>이 화면이 담는 기준은 홈과 같다. <b>Grafana 가 못 하는 것.</b> 적체 <i>건수</i>는
 * 이미 게이지로 나가 있다({@code outbox_dead_letter_count},
 * {@code settlement_deadletter_count}). 여기서 하는 것은 그 숫자로는 알 수 없는 두 가지다.
 * <b>무엇이 왜 막혔는지(원문)</b>와 <b>그래서 어떻게 할지(재처리·폐기)</b>.
 *
 * <p>같은 이유로 Redis Stream pending 은 담지 않았다. 그것도 이미 게이지가 있고,
 * 밀린 메시지는 {@code SettlementPendingSweeper} 가 30초마다 스스로 회수한다. 사람이
 * 볼 필요도 할 일도 없는 숫자를 화면에 두면 나머지 숫자의 값어치까지 떨어진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpsService {

    private static final int MIGRATION_LIMIT = 30;

    private final OutboxEventRepository outboxEventRepository;
    private final SettlementDeadLetterJpaRepository settlementDeadLetterRepository;
    private final MigrationHistoryRepository migrationHistoryRepository;

    /** 두 큐를 합친 적체 건수. 홈의 처리 대기 칸이 쓴다. */
    public long countDeadLetters() {
        return outboxEventRepository.countByStatus(OutboxStatus.DEAD_LETTER) + settlementDeadLetterRepository.count();
    }

    /**
     * 격리 메시지 목록.
     *
     * <p>두 테이블을 SQL 로 합치지 않는다. 스키마도 페이징 기준도 달라 UNION 하려면
     * 양쪽을 억지로 같은 모양으로 캐스팅해야 하고, 그러면 어느 쪽 컬럼이 무엇이었는지가
     * 쿼리 안에서 사라진다. 화면도 실제로는 한 번에 한 종류만 본다.
     */
    public Page<DeadLetter> findDeadLetters(DeadLetterSource source, int page, int size) {
        final PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        if (source == DeadLetterSource.OUTBOX) {
            return outboxEventRepository
                    .findByStatus(OutboxStatus.DEAD_LETTER, pageable)
                    .map(OpsService::toDeadLetter);
        }
        return settlementDeadLetterRepository.findAll(pageable).map(OpsService::toDeadLetter);
    }

    /**
     * 다시 큐에 넣는다. outbox 만 된다.
     *
     * <p>상태만 PENDING 으로 되돌리면 릴레이가 다음 폴링에서 집어 간다. 재시도 횟수는
     * <b>０으로 되돌리지 않는다.</b> 그래야 원인을 못 고친 채로 다시 넣었을 때 열 번을
     * 새로 쓰지 않고 곧바로 DLQ 로 돌아온다. 같은 메시지가 무한히 큐를 도는 것보다
     * 빨리 격리되는 편이 낫다.
     */
    @Transactional
    public void requeue(long outboxEventId) {
        final OutboxEvent event = findDeadLetter(outboxEventId);

        event.setStatusPending();
        log.info(
                "[Ops] outbox 격리 메시지를 다시 큐에 넣음. id={} streamKey={} retryCount={}",
                event.getId(),
                event.getStreamKey(),
                event.getRetryCount());
    }

    /**
     * 폐기. 행을 지운다.
     *
     * <p>두 테이블 모두 "폐기됨" 상태 컬럼이 없어 물리 삭제뿐이다. 컬럼을 새로 만들지
     * 않은 이유는, 폐기한 메시지를 계속 들고 있어 봐야 목록만 길어지고 아무도 다시 보지
     * 않기 때문이다. 대신 <b>누가 언제 지웠는지는 감사 로그에 남는다</b> - 이 API 는
     * DELETE 라 {@code AdminAuditAspect} 가 자동으로 기록한다.
     *
     * <p>지우기 전에 <b>격리된 메시지가 맞는지 확인한다.</b> outbox 테이블에는 발행을
     * 기다리는 PENDING 행과 이미 발행된 PUBLISHED 행이 함께 산다. id 만 보고 지우면
     * 아직 나가지 않은 도메인 이벤트를 삭제할 수 있고, 그건 outbox 를 둔 이유(메시지를
     * 잃지 않는 것)를 정면으로 깨뜨린다. 화면은 격리 목록만 보여주지만 API 는 id 를 직접
     * 받는다.
     *
     * <p>정산 쪽은 테이블 자체가 격리 전용이라 상태를 볼 것이 없다. 대신 없는 id 는
     * 404 로 돌려준다. {@code deleteById} 는 없는 행을 조용히 넘기므로, 그대로 두면
     * 일어나지 않은 삭제가 감사 로그에 성공으로 남는다.
     */
    @Transactional
    public void discard(DeadLetterSource source, long id) {
        if (source == DeadLetterSource.OUTBOX) {
            outboxEventRepository.delete(findDeadLetter(id));
            return;
        }

        settlementDeadLetterRepository.delete(
                settlementDeadLetterRepository.findById(id).orElseThrow(() -> notFound(id)));
    }

    /**
     * 격리된 outbox 메시지를 집는다. 없거나 격리 상태가 아니면 거절한다.
     *
     * <p>다시 넣기와 폐기가 같은 확인을 쓴다. 한쪽에만 두면 다른 쪽이 조용히 뚫린다.
     * 실제로 폐기가 그렇게 뚫려 있었다.
     */
    private OutboxEvent findDeadLetter(long outboxEventId) {
        final OutboxEvent event =
                outboxEventRepository.findById(outboxEventId).orElseThrow(() -> notFound(outboxEventId));

        if (event.getStatus() != OutboxStatus.DEAD_LETTER) {
            throw new BusinessException(OpsErrorCode.NOT_DEAD_LETTER, "격리 상태가 아닌 메시지입니다. 현재 상태=" + event.getStatus());
        }
        return event;
    }

    private static BusinessException notFound(long id) {
        return new BusinessException(OpsErrorCode.DEAD_LETTER_NOT_FOUND, "존재하지 않는 격리 메시지입니다. id=" + id);
    }

    public List<MigrationRecord> migrations() {
        return migrationHistoryRepository.findAll(MIGRATION_LIMIT);
    }

    public boolean migrationHistoryExists() {
        return migrationHistoryRepository.exists();
    }

    private static DeadLetter toDeadLetter(OutboxEvent event) {
        return new DeadLetter(
                DeadLetterSource.OUTBOX,
                event.getId(),
                event.getStreamKey(),
                // outbox 는 실패 사유를 저장하지 않는다. 재시도 횟수가 유일한 단서라 그걸 문장으로 만든다.
                "발행 재시도 " + event.getRetryCount() + "회 소진",
                event.getPayload(),
                event.getRetryCount(),
                event.getCreatedAt());
    }

    private static DeadLetter toDeadLetter(SettlementDeadLetterEntity entity) {
        return new DeadLetter(
                DeadLetterSource.SETTLEMENT,
                entity.getId(),
                entity.getRecordId(),
                entity.getReason(),
                entity.getPayload(),
                null,
                // 이 테이블만 LocalDateTime 이다. 서버 시간대로 해석해 다른 응답과 타입을 맞춘다.
                entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
    }
}
