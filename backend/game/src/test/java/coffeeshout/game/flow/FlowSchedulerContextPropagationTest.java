package coffeeshout.game.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.game.scheduler.GameTaskSchedulerFactory;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * GameTaskSchedulerFactory가 생성한 스케줄러에서 Flow 체인의 지연 실행이
 * 제출 시점의 ThreadLocal 컨텍스트를 보는지 검증한다.
 *
 * <p>프로덕션에서는 이 메커니즘으로 트레이스 컨텍스트가 전파되어,
 * 지연 실행 후 Stream 발행 시 traceparent가 끊기지 않는다.</p>
 */
class FlowSchedulerContextPropagationTest {

    private static final ThreadLocal<String> TRACE = new ThreadLocal<>();

    private GameTaskSchedulerFactory schedulerFactory;
    private ThreadPoolTaskScheduler scheduler;
    private CompletableFutureFlowScheduler flowScheduler;

    @BeforeEach
    void setUp() {
        ContextRegistry registry = new ContextRegistry();
        registry.registerThreadLocalAccessor("test-trace", TRACE::get, TRACE::set, TRACE::remove);
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
                .contextRegistry(registry)
                .build();

        schedulerFactory = new GameTaskSchedulerFactory(snapshotFactory);
        scheduler = schedulerFactory.create("test-task-", 2, "테스트 스케줄 실행 중 예외가 발생했습니다.");
        flowScheduler = new CompletableFutureFlowScheduler(scheduler);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
        TRACE.remove();
    }

    @Nested
    class 팩토리가_생성한_스케줄러는 {

        @Test
        void schedule된_action이_제출_시점의_컨텍스트를_본다() {
            // given
            TRACE.set("trace-schedule");
            AtomicReference<String> seenInAction = new AtomicReference<>();

            // when
            flowScheduler.schedule(() -> seenInAction.set(TRACE.get()), Duration.ofMillis(10));

            // then
            await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(seenInAction.get()).isEqualTo("trace-schedule"));
        }

        @Test
        void andThen_체인의_후속_action까지_컨텍스트가_전이된다() {
            // given
            TRACE.set("trace-chain");
            AtomicReference<String> seenInSecondStep = new AtomicReference<>();

            // when — 첫 스텝 완료 후 스케줄러 스레드에서 두 번째 스텝이 제출된다
            flowScheduler.schedule(() -> {
                    }, Duration.ofMillis(10))
                    .andThen(() -> seenInSecondStep.set(TRACE.get()), Duration.ofMillis(10));

            // then
            await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(seenInSecondStep.get()).isEqualTo("trace-chain"));
        }

        @Test
        void 컨텍스트가_없으면_action도_빈_컨텍스트로_실행된다() {
            // given — 제출 스레드에 컨텍스트 없음
            TRACE.remove();
            AtomicReference<String> seenInAction = new AtomicReference<>("초기값");

            // when
            flowScheduler.schedule(() -> seenInAction.set(TRACE.get()), Duration.ofMillis(10));

            // then
            await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(seenInAction.get()).isNull());
        }
    }

    @Nested
    class create_메서드는 {

        @Test
        void poolSize가_1_미만이면_1로_보정한다() {
            // when
            ThreadPoolTaskScheduler created = schedulerFactory.create("min-pool-", 0, "예외");

            // then — getPoolSize()는 현재 스레드 수를 반환하므로 corePoolSize로 검증한다
            assertThat(created.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
            created.shutdown();
        }

        @Test
        void 스레드_이름_접두사를_적용한다() {
            // given
            AtomicReference<String> threadName = new AtomicReference<>();
            ThreadPoolTaskScheduler created = schedulerFactory.create("prefix-test-", 1, "예외");

            // when
            created.execute(() -> threadName.set(Thread.currentThread().getName()));

            // then
            await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(threadName.get()).startsWith("prefix-test-"));
            created.shutdown();
        }
    }
}
