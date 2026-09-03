package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

import coffeeshout.global.lock.RedisLock;
import coffeeshout.profanity.application.ProfanityAuditService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 12시간 주기 검열 스케줄러의 구조적 결함 두 가지를 고정한다. 둘 다 실사용자 트래픽 없이 재현된다.
 */
class NicknameAuditSchedulerTest {

    /**
     * 결함 1 — 감사 작업이 스케줄러 스레드에서 동기로 돌면 안 된다.
     *
     * <p>프로덕션에는 {@code taskScheduler}라는 이름의 빈이 없고 {@code DelayRemovalSchedulerConfig}가
     * 정의한 {@code TaskScheduler} 빈 하나만 있다. Spring Boot의 {@code TaskSchedulingAutoConfiguration}이
     * {@code @ConditionalOnMissingBean(TaskScheduler.class)}이라 물러나면서 모든 {@code @Scheduled}가
     * 풀 크기 1로 수렴하고, 그 안에 500ms 주기 Outbox 릴레이가 있다.
     *
     * <p>검열 회차는 적체량에 비례해 분에서 시간 단위로 길어지므로, 스케줄러 스레드에서 직접 돌리면
     * 그동안 앱의 모든 스케줄 작업이 멈춘다.
     */
    @Nested
    class 스케줄러_스레드_점유 {

        private static final long DISPATCH_TIMEOUT_SECONDS = 5;

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

            final ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                final NicknameAuditScheduler scheduler = new NicknameAuditScheduler(auditService, executor);
                final Thread schedulerThread = Thread.currentThread();

                scheduler.auditPendingNicknames();
                auditStarted.await(DISPATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                assertThat(executingThread.get())
                        .as("감사 작업을 실행한 스레드. 스케줄러 스레드와 같으면 회차가 끝날 때까지" + " Outbox 릴레이를 포함한 모든 @Scheduled 작업이 대기한다.")
                        .isNotNull()
                        .isNotSameAs(schedulerThread);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    /**
     * 결함 2 — 분산 락이 없으면 인스턴스 여러 대가 같은 큐를 동시에 읽는다.
     *
     * <p>프로덕션은 Blue/Green이다. 전환 구간에는 두 컨테이너가 동시에 살아 있고, 그때 cron이 걸리면
     * 두 인스턴스가 큐의 같은 0페이지를 읽는다. 레이트리미터는 JVM 로컬이라 실제 RPM이 두 배가 되어
     * Gemini 무료 티어 한도를 넘고, 429가 재시도를 소진시켜 배치가 통째로 skip된다.
     *
     * <p>락은 스케줄러가 아니라 {@link ProfanityAuditService#auditPending()}에 있어야 한다. 스케줄러는
     * 작업을 실행기로 넘기고 즉시 반환하므로, 거기 걸면 넘기자마자 락이 풀려 아무것도 지키지 못한다.
     */
    @Nested
    class 다중_인스턴스_중복_실행 {

        /** 회차 예산 기본값 10분. leaseTime 은 이보다 길어야 실행 중 락이 풀리지 않는다. */
        private static final long MAX_RUN_MILLIS = 600_000L;

        private RedisLock auditPendingLock() throws NoSuchMethodException {
            return ProfanityAuditService.class.getDeclaredMethod("auditPending").getAnnotation(RedisLock.class);
        }

        @Test
        void 검열_작업은_분산_락으로_보호되어야_한다() throws NoSuchMethodException {
            assertThat(auditPendingLock())
                    .as("Blue/Green 전환 구간에 두 인스턴스가 같은 큐를 동시에 검열하는 것을 막는 락이 없다.")
                    .isNotNull();
        }

        @Test
        void 락_유지시간은_회차_예산보다_길어야_한다() throws NoSuchMethodException {
            assertThat(auditPendingLock().leaseTime())
                    .as("Redisson 은 leaseTime 을 명시하면 워치독 갱신 없이 그 시간 뒤 락을 자동 해제한다."
                            + " 회차 예산보다 짧으면 실행 도중 락이 풀려 다른 인스턴스가 끼어든다.")
                    .isGreaterThan(MAX_RUN_MILLIS);
        }

        @Test
        void 완료_마킹_TTL은_cron_주기보다_짧아야_한다() throws NoSuchMethodException {
            assertThat(auditPendingLock().doneTtl())
                    .as("완료 마킹이 cron 주기(12시간)보다 오래 남으면 다음 회차가 조용히 건너뛰어진다.")
                    .isLessThan(TimeUnit.HOURS.toMillis(12));
        }
    }
}
