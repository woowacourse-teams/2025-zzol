package coffeeshout.profanity.application;

import coffeeshout.global.lock.RedisLock;
import coffeeshout.global.nickname.NicknameSubmittedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityAuditService {

    /**
     * Redisson은 leaseTime을 명시하면 워치독 갱신 없이 그 시간 뒤 락을 자동 해제한다.
     * 회차 예산({@code nickname-audit.max-run-duration}, 기본 10분)보다 넉넉히 길어야
     * 실행 도중 락이 풀려 다른 인스턴스가 같은 큐에 끼어드는 일을 막는다.
     */
    private static final long LOCK_LEASE_MILLIS = 900_000L;

    /** 주기 작업이라 완료 마킹은 짧게 둔다. cron 주기(12시간)보다 길면 다음 회차가 조용히 건너뛰어진다. */
    private static final long LOCK_DONE_TTL_MILLIS = 60_000L;

    private final NicknameAuditRepository auditRepository;
    private final ProfanityAuditBatchProcessor batchProcessor;
    private final ProfanityWordManagementService profanityWordManagementService;
    private final NicknameAuditProperties properties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    private final AtomicLong unauditedQueueDepth = new AtomicLong(0);

    @PostConstruct
    void initMetrics() {
        Gauge.builder("nickname.audit.unaudited.queue", unauditedQueueDepth, AtomicLong::get)
                .description("스케줄러 실행 시점의 UNAUDITED 닉네임 적체량")
                .register(meterRegistry);
    }

    /**
     * 닉네임 저장 트랜잭션에 참여해 검열 대기 행을 함께 커밋한다 (트랜잭셔널 아웃박스, #1618).
     * <p>
     * AFTER_COMMIT + REQUIRES_NEW였을 때는 닉네임이 커밋된 뒤 별개 트랜잭션으로 큐에 적재해서,
     * 그 사이 인스턴스가 죽으면 해당 닉네임이 검열 큐에 영영 들어가지 않았다. 미검열 닉네임을
     * 다시 주워담는 복구 스윕도 없어 유실이 곧 영구 누락이었다.
     */
    @EventListener
    @Transactional
    public void onNicknameSubmitted(NicknameSubmittedEvent event) {
        log.debug("닉네임 검열 등록 요청 수신: {}", event.nickname());
        register(event.nickname());
    }

    public Page<NicknameAudit> listByStatus(NicknameAuditStatus status, Pageable pageable) {
        return auditRepository.findByStatus(status, pageable);
    }

    /**
     * 트랜잭션을 요구한다. {@code insertUnaudited}가 {@code @Modifying} 네이티브 쿼리라
     * 주변 트랜잭션이 없으면 {@code TransactionRequiredException}으로 실패한다 —
     * {@code save()}가 리포지토리 자체 트랜잭션으로 동작하던 것과 달라진 지점이다.
     * ({@code OutboxEventRecorder.record()}와 같은 이유로 REQUIRED를 명시한다)
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void register(String nickname) {
        if (profanityWordManagementService.isOperatorAllowed(nickname)) {
            log.debug("운영자 허용 닉네임 — 검열 등록 생략: {}", nickname);
            return;
        }
        if (auditRepository.existsByNickname(nickname)) {
            // 상태 무관으로 검사한다. 유니크 제약이 (player_name, status)라 이미 검열된(CLEAN 등)
            // 닉네임에 대해 UNAUDITED만 검사하면 새 UNAUDITED 중복이 생기고, 다음 검열 시
            // 기존 상태로 승격되며 유니크 충돌이 발생한다 (issue #1467).
            log.debug("이미 등록된 닉네임 — 검열 등록 생략: {}", nickname);
            return;
        }
        // save가 아니라 충돌 무시 INSERT다. 위 조회를 통과한 동시 등록 둘이 겹치면 유니크 제약에
        // 걸리는데, 이제 호출자(닉네임 변경·룰렛 결과 저장) 트랜잭션 안이라 예외가 나면 그쪽까지
        // 롤백된다. 충돌은 "이미 큐에 있다"는 뜻이라 무시해도 손실이 없다.
        auditRepository.insertUnaudited(nickname, clock.instant());
    }

    /**
     * 미검열 닉네임을 배치로 검열한다.
     *
     * <p>회차에 시간 예산을 둔다. 배치 하나가 Gemini 호출 하나이고 레이트리미터가 13초에 하나만
     * 허용하므로 소요 시간이 적체량에 정비례한다. 상한이 없으면 적체 10만 건에 3.6시간을 도는데,
     * 그동안 실행기 스레드를 붙잡고 있게 된다.
     *
     * <p>분산 락은 여기 건다. 스케줄러는 작업을 실행기로 넘기고 즉시 반환하므로 그쪽에 걸면
     * 락이 바로 풀린다. Blue/Green 전환 구간에 두 인스턴스가 같은 큐를 읽는 것을 막는 게 목적이다.
     */
    @RedisLock(
            key = "'nickname-audit'",
            lockPrefix = "lock:",
            donePrefix = "done:",
            waitTime = 0,
            leaseTime = LOCK_LEASE_MILLIS,
            doneTtl = LOCK_DONE_TTL_MILLIS)
    public void auditPending() {
        final long initialQueueSize = auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED);
        unauditedQueueDepth.set(initialQueueSize);
        log.info("닉네임 검열 시작: UNAUDITED 적체량 {}건", initialQueueSize);

        final Instant deadline = clock.instant().plus(properties.maxRunDuration());
        final Pageable pageable =
                PageRequest.of(0, properties.batchSize(), Sort.by("createdAt").ascending());
        List<NicknameAudit> batch =
                auditRepository.findByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED, pageable);
        int processedTotal = 0;

        while (!batch.isEmpty()) {
            // processed는 실제로 UNAUDITED에서 빠져나간 행 수다. 진행이 없으면 같은 0페이지를 영원히
            // 다시 읽게 되므로 0이면 멈춘다.
            int processed = batchProcessor.process(batch);
            processedTotal += processed;
            log.info("닉네임 검열 진행: 이번 배치 {}건 중 {}건 처리, 누적 {}건", batch.size(), processed, processedTotal);

            if (processed == 0 || batch.size() < properties.batchSize()) {
                break;
            }
            if (!clock.instant().isBefore(deadline)) {
                log.warn(
                        "닉네임 검열 회차 시간 예산({}) 소진 — 남은 적체는 다음 회차로 넘긴다. 누적 {}건",
                        properties.maxRunDuration(),
                        processedTotal);
                break;
            }
            batch = auditRepository.findByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED, pageable);
        }

        log.info("닉네임 검열 완료: 총 {}건 처리", processedTotal);
    }
}
