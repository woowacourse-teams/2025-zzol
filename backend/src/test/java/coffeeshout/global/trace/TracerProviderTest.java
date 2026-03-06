package coffeeshout.global.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import coffeeshout.global.redis.BaseEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.test.simple.SimpleTracer;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TracerProviderTest {

    private SimpleTracer simpleTracer;
    private TracerProvider tracerProvider;

    @BeforeEach
    void setUp() {
        simpleTracer = new SimpleTracer();
        tracerProvider = new TracerProvider(simpleTracer);
    }

    @Nested
    class executeWithTraceContext_호출_시 {

        @Test
        void 유효한_TraceInfo가_주어지면_해당_traceId로_Span이_생성된다() {
            // given
            String expectedTraceId = "463ac35c9f6413ad48485a3953bb6124";
            String expectedSpanId = "0020000000000001";
            TraceInfo traceInfo = new TraceInfo(expectedTraceId, expectedSpanId);

            AtomicReference<String> capturedTraceId = new AtomicReference<>();

            // when
            tracerProvider.executeWithTraceContext(traceInfo, () -> {
                Span currentSpan = simpleTracer.currentSpan();
                capturedTraceId.set(currentSpan.context().traceId());
            }, new StubEvent());

            // then
            assertThat(capturedTraceId.get()).isEqualTo(expectedTraceId);
        }

        @Test
        void 빈_TraceInfo가_주어지면_Span_생성_없이_task를_바로_실행한다() {
            // given
            TraceInfo emptyTraceInfo = new TraceInfo("", "");
            AtomicReference<Boolean> executed = new AtomicReference<>(false);

            // when
            tracerProvider.executeWithTraceContext(emptyTraceInfo, () -> executed.set(true), new StubEvent());

            // then
            assertThat(executed.get()).isTrue();
            assertThat(simpleTracer.getSpans()).isEmpty();
        }

        @Test
        void task_실행_후_Span이_정상적으로_종료된다() {
            // given
            TraceInfo traceInfo = new TraceInfo("463ac35c9f6413ad48485a3953bb6124", "0020000000000001");

            // when
            tracerProvider.executeWithTraceContext(traceInfo, () -> {}, new StubEvent());

            // then
            assertThat(simpleTracer.getSpans()).hasSize(1);
            assertThat(simpleTracer.getSpans().getFirst().getEndTimestamp()).isNotNull();
        }

        @Test
        void task_내부에서_예외가_발생해도_Span은_종료된다() {
            // given
            TraceInfo traceInfo = new TraceInfo("463ac35c9f6413ad48485a3953bb6124", "0020000000000001");

            // when & then
            assertThatCode(() ->
                    tracerProvider.executeWithTraceContext(traceInfo, () -> {
                        throw new RuntimeException("의도된 예외");
                    }, new StubEvent())
            ).isInstanceOf(RuntimeException.class);

            assertThat(simpleTracer.getSpans()).isNotEmpty();
        }

        @Test
        void Span_이름은_이벤트_클래스의_SimpleName이다() {
            // given
            TraceInfo traceInfo = new TraceInfo("463ac35c9f6413ad48485a3953bb6124", "0020000000000001");

            // when
            tracerProvider.executeWithTraceContext(traceInfo, () -> {}, new StubEvent());

            // then
            assertThat(simpleTracer.getSpans().getFirst().getName()).isEqualTo("StubEvent");
        }

        @Test
        void 별도_스레드에서_실행해도_전달받은_traceId가_해당_스레드에_복원된다() throws InterruptedException {
            // given
            String expectedTraceId = "463ac35c9f6413ad48485a3953bb6124";
            TraceInfo traceInfo = new TraceInfo(expectedTraceId, "0020000000000001");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> capturedTraceId = new AtomicReference<>();
            ExecutorService executor = Executors.newSingleThreadExecutor();

            // when — 다른 스레드에서 executeWithTraceContext 호출
            executor.submit(() -> {
                tracerProvider.executeWithTraceContext(traceInfo, () -> {
                    capturedTraceId.set(simpleTracer.currentSpan().context().traceId());
                }, new StubEvent());
                latch.countDown();
            });

            // then — CountDownLatch로 스레드 완료 대기 (Thread.sleep 아님)
            assertThat(latch.await(3, TimeUnit.SECONDS))
                    .as("비동기 태스크가 3초 이내에 완료되어야 한다")
                    .isTrue();
            assertThat(capturedTraceId.get()).isEqualTo(expectedTraceId);

            executor.shutdown();
        }
    }

    private static class StubEvent implements BaseEvent {
        @Override
        public String eventId() {
            return "stub-event-id";
        }

        @Override
        public Instant timestamp() {
            return Instant.now();
        }
    }
}
