package coffeeshout.game.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
 * 게임 스케줄러 설정과 동일하게 TaskDecorator(ContextSnapshot)를 적용한 스케줄러에서
 * Flow 체인의 지연 실행이 제출 시점의 ThreadLocal 컨텍스트를 보는지 검증한다.
 *
 * <p>프로덕션에서는 이 메커니즘으로 트레이스 컨텍스트가 전파되어,
 * 지연 실행 후 Stream 발행 시 traceparent가 끊기지 않는다.</p>
 */
class FlowSchedulerContextPropagationTest {

    private static final ThreadLocal<String> TRACE = new ThreadLocal<>();

    private ThreadPoolTaskScheduler scheduler;
    private CompletableFutureFlowScheduler flowScheduler;

    @BeforeEach
    void setUp() {
        ContextRegistry registry = new ContextRegistry();
        registry.registerThreadLocalAccessor("test-trace", TRACE::get, TRACE::set, TRACE::remove);
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
                .contextRegistry(registry)
                .build();

        // 게임 스케줄러 설정(*SchedulerConfig)과 동일한 데코레이터 구성
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setTaskDecorator(runnable -> snapshotFactory.captureAll().wrap(runnable));
        scheduler.initialize();

        flowScheduler = new CompletableFutureFlowScheduler(scheduler);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
        TRACE.remove();
    }

    @Nested
    class 데코레이터가_적용된_스케줄러는 {

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
}
