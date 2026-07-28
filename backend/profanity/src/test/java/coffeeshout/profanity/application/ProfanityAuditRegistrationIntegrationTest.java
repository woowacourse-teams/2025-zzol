package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import coffeeshout.global.nickname.NicknameSubmittedEvent;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditJpaRepository;
import coffeeshout.support.IntegrationTestSupport;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
    private ProfanityAuditService service;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void 트랜잭션_없는_호출자도_검열_등록에_성공한다() {
        // insertUnaudited는 @Modifying 네이티브 쿼리라 주변 트랜잭션이 없으면 TransactionRequiredException이다.
        // save()는 리포지토리 자체 트랜잭션으로 동작했으므로, register()가 스스로 트랜잭션을 열지 않으면
        // 어드민·초기화 코드처럼 트랜잭션 밖에서 부르는 호출자가 조용히 깨진다.
        assertThatCode(() -> service.register("비트랜잭션")).doesNotThrowAnyException();

        assertThat(auditRepository.findNicknamesByStatus(NicknameAuditStatus.UNAUDITED))
                .containsOnly("비트랜잭션");
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
    void 서로_다른_트랜잭션이_같은_닉네임을_동시에_등록해도_INSERT가_예외를_던지지_않는다() {
        // 조회 가드(existsByNickname)를 둘 다 통과한 뒤 서로 다른 트랜잭션이 같은 행을 INSERT하는
        // 상황이다 — 방 참가 닉네임은 전역 유니크가 아니라 서로 다른 방에서 같은 이름이 동시에
        // 당첨될 수 있다. 예전 save()였다면 늦게 커밋하는 쪽이 uq_player_name_audit_name_status에
        // 걸리고, 이제 검열 등록이 호출자 트랜잭션 안이라 룰렛 결과 저장까지 함께 롤백됐다.
        // 여기서 검증하는 건 그 예외가 애초에 발생하지 않는다는 것이다.
        //
        // 가드 통과를 래치로 맞춰야 의미가 있다. 한쪽이 먼저 커밋해버리면 다른 쪽 가드가 그 행을
        // 보고 INSERT를 건너뛰어 충돌 자체가 일어나지 않는다.
        final CountDownLatch guardPassed = new CountDownLatch(2);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Runnable register = () -> transactionTemplate.executeWithoutResult(status -> {
                try {
                    assertThat(auditRepository.existsByNickname("동시닉네임")).isFalse();
                } finally {
                    // 단언이 실패해도 반대 스레드를 래치에서 굶기지 않는다.
                    guardPassed.countDown();
                }
                awaitBothGuardsPassed(guardPassed);
                auditRepository.insertUnaudited("동시닉네임", Instant.now());
            });
            final Future<?> first = executor.submit(register);
            final Future<?> second = executor.submit(register);

            assertThatCode(() -> first.get(10, TimeUnit.SECONDS)).doesNotThrowAnyException();
            assertThatCode(() -> second.get(10, TimeUnit.SECONDS)).doesNotThrowAnyException();
        } finally {
            executor.shutdownNow();
        }

        assertThat(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED)).isEqualTo(1);
    }

    private void awaitBothGuardsPassed(CountDownLatch guardPassed) {
        try {
            if (!guardPassed.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("두 트랜잭션이 모두 조회 가드를 통과하지 못했습니다");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
