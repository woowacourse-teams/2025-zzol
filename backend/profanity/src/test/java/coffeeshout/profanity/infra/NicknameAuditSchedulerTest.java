package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

import coffeeshout.global.lock.RedisLock;
import coffeeshout.profanity.application.ProfanityAuditService;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 재현 테스트 — 12시간 주기 검열 스케줄러의 구조적 결함 두 가지.
 *
 * <p>둘 다 실사용자 트래픽 없이 재현된다. 지금 프로덕션에 잠복해 있고, 조건이 맞으면 오늘 터진다.
 */
class NicknameAuditSchedulerTest {

    /**
     * 결함 1 — 감사 작업이 스케줄러 스레드에서 동기로 돈다.
     *
     * <p>프로덕션에는 {@code taskScheduler}라는 이름의 빈이 없고, {@code DelayRemovalSchedulerConfig}가
     * 정의한 {@code TaskScheduler} 빈 하나만 있다. Spring Boot의 {@code TaskSchedulingAutoConfiguration}은
     * {@code @ConditionalOnMissingBean(TaskScheduler.class)}이라 물러나고, 결과적으로 모든 {@code @Scheduled}가
     * 풀 크기 1로 수렴한다. 그 안에 500ms 주기 Outbox 릴레이가 있다.
     *
     * <p>따라서 {@code auditPendingNicknames()}가 반환하지 않는 동안 앱의 모든 스케줄 작업이 멈춘다.
     * 적체 1만 건이면 21분, 10만 건이면 3.6시간이다. Gemini 응답이 끝내 오지 않으면 무기한이다
     * ({@code NicknameAuditConfig}가 HTTP 타임아웃을 설정하지 않는다).
     *
     * <p>수정 방향은 감사 작업을 이미 있는 {@code virtualThreadExecutor}로 넘기고 스케줄러 메서드는
     * 즉시 반환하게 하는 것이다.
     */
    @Nested
    class 스케줄러_스레드_점유 {

        private static final long DISPATCH_TIMEOUT_SECONDS = 1;

        @Test
        void 감사_작업은_스케줄러_스레드와_다른_스레드에서_돌아야_한다() throws InterruptedException {
            final CountDownLatch auditStarted = new CountDownLatch(1);
            final AtomicReference<Thread> executingThread = new AtomicReference<>();
            final ProfanityAuditService auditService = mock(ProfanityAuditService.class);
            willAnswer(invocation -> {
                        executingThread.set(Thread.currentThread());
                        auditStarted.countDown();
                        return null;
                    })
                    .given(auditService)
                    .auditPending();
            final NicknameAuditScheduler scheduler = new NicknameAuditScheduler(auditService);
            final Thread schedulerThread = Thread.currentThread();

            scheduler.auditPendingNicknames();
            auditStarted.await(DISPATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(executingThread.get())
                    .as("감사 작업을 실행한 스레드. 스케줄러 스레드와 같으면 회차가 끝날 때까지" + " Outbox 릴레이를 포함한 모든 @Scheduled 작업이 대기한다.")
                    .isNotNull()
                    .isNotSameAs(schedulerThread);
        }
    }

    /**
     * 결함 2 — 분산 락이 없어 인스턴스 여러 대가 같은 큐를 동시에 읽는다.
     *
     * <p>프로덕션은 Blue/Green이다({@code docker/prod/docker-compose.yml}). 전환 구간에는 두 컨테이너가
     * 동시에 살아 있고, 그때 cron(00:00·12:00)이 걸리면 두 인스턴스가 큐의 같은 0페이지를 읽는다.
     * 레이트리미터는 JVM 로컬이라 실제 RPM이 두 배가 되어 Gemini 무료 티어 한도 5를 넘고,
     * 429가 재시도를 소진시켜 배치가 통째로 skip된다. 같은 닉네임에 대해 자동 차단도 중복 발동한다.
     *
     * <p>같은 저장소의 다른 경로들은 이미 {@code @RedisLock}을 쓴다
     * ({@code RoulettePersistenceService}, {@code MiniGamePersistenceService}). 검열 스케줄러만 맨몸이다.
     *
     * <p>주의 — 수정할 때 {@code leaseTime} 기본값 5초를 그대로 쓰면 안 된다. 감사 회차는 분에서 시간
     * 단위라 실행 중에 락이 만료되고, 그 사이 다른 인스턴스가 락을 새로 잡는다. 회차 상한(결함 1의 시간 예산)에
     * 맞춰 명시해야 한다.
     */
    @Nested
    class 다중_인스턴스_중복_실행 {

        @Test
        void 검열_스케줄러는_분산_락으로_보호되어야_한다() throws NoSuchMethodException {
            final Method scheduledMethod = NicknameAuditScheduler.class.getDeclaredMethod("auditPendingNicknames");

            final RedisLock redisLock = scheduledMethod.getAnnotation(RedisLock.class);

            assertThat(redisLock)
                    .as("Blue/Green 전환 구간에 두 인스턴스가 같은 큐를 동시에 검열하는 것을 막는 락이 없다.")
                    .isNotNull();
        }
    }
}
