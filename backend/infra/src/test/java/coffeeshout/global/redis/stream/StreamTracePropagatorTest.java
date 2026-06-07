package coffeeshout.global.redis.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamTracePropagatorTest {

    private static final String TRACEPARENT = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";

    @Mock
    private Tracer tracer;

    @Mock
    private Propagator propagator;

    @InjectMocks
    private StreamTracePropagator streamTracePropagator;

    private void 활성_스팬을_설정한다(Span span, TraceContext traceContext) {
        given(tracer.currentSpan()).willReturn(span);
        given(span.context()).willReturn(traceContext);
    }

    private void inject가_traceparent를_쓰도록_설정한다(TraceContext traceContext) {
        willAnswer(invocation -> {
            Map<String, String> carrier = invocation.getArgument(1);
            carrier.put(StreamRecordFields.TRACEPARENT, TRACEPARENT);
            return null;
        }).given(propagator).inject(eq(traceContext), any(), any());
    }

    @Nested
    class injectCurrentContext_메서드는 {

        @Test
        void 활성_스팬이_없으면_캐리어를_변경하지_않는다() {
            // given
            given(tracer.currentSpan()).willReturn(null);
            Map<String, String> carrier = new HashMap<>();

            // when
            streamTracePropagator.injectCurrentContext(carrier);

            // then
            assertThat(carrier).isEmpty();
            verify(propagator, never()).inject(any(), any(), any());
        }

        @Test
        void 활성_스팬이_있으면_해당_컨텍스트를_캐리어에_주입한다(@Mock Span span, @Mock TraceContext traceContext) {
            // given
            활성_스팬을_설정한다(span, traceContext);
            inject가_traceparent를_쓰도록_설정한다(traceContext);
            Map<String, String> carrier = new HashMap<>();

            // when
            streamTracePropagator.injectCurrentContext(carrier);

            // then
            assertThat(carrier).containsEntry(StreamRecordFields.TRACEPARENT, TRACEPARENT);
        }
    }

    @Nested
    class currentTraceparent_메서드는 {

        @Test
        void 활성_스팬이_없으면_null을_반환한다() {
            // given
            given(tracer.currentSpan()).willReturn(null);

            // when & then
            assertThat(streamTracePropagator.currentTraceparent()).isNull();
        }

        @Test
        void 활성_스팬이_있으면_traceparent_헤더_값을_반환한다(@Mock Span span, @Mock TraceContext traceContext) {
            // given
            활성_스팬을_설정한다(span, traceContext);
            inject가_traceparent를_쓰도록_설정한다(traceContext);

            // when & then
            assertThat(streamTracePropagator.currentTraceparent()).isEqualTo(TRACEPARENT);
        }
    }

    @Nested
    class runInConsumerScope_메서드는 {

        @Test
        void traceparent가_없으면_스팬_복원_없이_task만_실행한다() {
            // given
            AtomicBoolean executed = new AtomicBoolean(false);

            // when
            streamTracePropagator.runInConsumerScope(Map.of(), "TestEvent", () -> executed.set(true));

            // then
            assertThat(executed).isTrue();
            verify(propagator, never()).extract(any(), any());
        }

        @Test
        void traceparent가_있으면_consumer_span_스코프_안에서_task를_실행하고_스팬을_종료한다(
                @Mock Span.Builder spanBuilder,
                @Mock Span span,
                @Mock Tracer.SpanInScope spanInScope
        ) {
            // given
            Map<String, String> carrier = Map.of(StreamRecordFields.TRACEPARENT, TRACEPARENT);
            given(propagator.extract(eq(carrier), any())).willReturn(spanBuilder);
            given(spanBuilder.name("TestEvent")).willReturn(spanBuilder);
            given(spanBuilder.kind(Span.Kind.CONSUMER)).willReturn(spanBuilder);
            given(spanBuilder.start()).willReturn(span);
            given(tracer.withSpan(span)).willReturn(spanInScope);

            AtomicBoolean executed = new AtomicBoolean(false);

            // when
            streamTracePropagator.runInConsumerScope(carrier, "TestEvent", () -> executed.set(true));

            // then
            assertThat(executed).isTrue();
            InOrder order = inOrder(spanInScope, span);
            order.verify(spanInScope).close();
            order.verify(span).end();
        }

        @Test
        void task가_예외를_던지면_스팬에_오류를_기록하고_종료한_뒤_재던진다(
                @Mock Span.Builder spanBuilder,
                @Mock Span span,
                @Mock Tracer.SpanInScope spanInScope
        ) {
            // given
            Map<String, String> carrier = Map.of(StreamRecordFields.TRACEPARENT, TRACEPARENT);
            given(propagator.extract(eq(carrier), any())).willReturn(spanBuilder);
            given(spanBuilder.name("TestEvent")).willReturn(spanBuilder);
            given(spanBuilder.kind(Span.Kind.CONSUMER)).willReturn(spanBuilder);
            given(spanBuilder.start()).willReturn(span);
            given(tracer.withSpan(span)).willReturn(spanInScope);

            RuntimeException failure = new RuntimeException("처리 실패");

            // when
            assertThatThrownBy(() -> streamTracePropagator.runInConsumerScope(
                    carrier, "TestEvent", () -> {
                        throw failure;
                    }))
                    .isSameAs(failure);

            // then
            verify(span).error(failure);
            verify(span).end();
        }
    }
}
