package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import coffeeshout.global.nickname.NicknameSubmittedEvent;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditJpaRepository;
import coffeeshout.support.IntegrationTestSupport;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 닉네임 검열 등록이 닉네임 저장 트랜잭션에 참여하는지 검증한다 (트랜잭셔널 아웃박스, #1618).
 *
 * <p>비트랜잭션 베이스({@link IntegrationTestSupport})를 상속해 실제 commit·rollback 경계에서
 * 확인한다. @Transactional 롤백 베이스에서는 트랜잭션 참여 여부 자체를 구분할 수 없다.
 */
class ProfanityAuditRegistrationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private NicknameAuditJpaRepository auditRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void 닉네임_저장_트랜잭션이_커밋되면_검열_대기_행도_함께_커밋된다() {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new NicknameSubmittedEvent("커밋닉네임")));

        assertThat(auditRepository.findNicknamesByStatus(NicknameAuditStatus.UNAUDITED))
                .containsOnly("커밋닉네임");
    }

    @Test
    void 검열_등록은_커밋_이후가_아니라_같은_트랜잭션_안에서_이뤄진다() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new NicknameSubmittedEvent("동일트랜잭션"));

            // 유실 창의 정체가 여기다. AFTER_COMMIT 리스너였다면 이 시점엔 아직 등록되지 않았고,
            // 커밋과 등록 사이에 인스턴스가 죽으면 그 닉네임은 검열 큐에 영영 들어가지 않았다.
            assertThat(auditRepository.findNicknamesByStatus(NicknameAuditStatus.UNAUDITED))
                    .containsOnly("동일트랜잭션");

            status.setRollbackOnly();
        });

        // 같은 트랜잭션이므로 비즈니스 롤백이 검열 등록까지 되돌린다.
        assertThat(auditRepository.findNicknamesByStatus(NicknameAuditStatus.UNAUDITED)).isEmpty();
    }

    @Test
    void 같은_닉네임이_동시에_등록돼도_유니크_충돌이_호출자_트랜잭션을_깨뜨리지_않는다() {
        // 조회 가드(existsByNickname)를 둘 다 통과한 동시 등록 상황. 예전 save()였다면 두 번째
        // INSERT가 uq_player_name_audit_name_status에 걸려, 이제 같은 트랜잭션을 쓰는 호출자
        // (닉네임 변경·룰렛 결과 저장)까지 함께 롤백됐다.
        assertThatCode(() -> transactionTemplate.executeWithoutResult(status -> {
            auditRepository.insertUnaudited("동시닉네임", Instant.now());
            auditRepository.insertUnaudited("동시닉네임", Instant.now());
        })).doesNotThrowAnyException();

        assertThat(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED)).isEqualTo(1);
    }
}
