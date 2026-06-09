package coffeeshout.global.redis.stream;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Redis Stream 경계의 트레이스 컨텍스트 전파를 담당한다.
 * <p>
 * 발행 측은 현재 컨텍스트를 W3C 캐리어 필드(traceparent, tracestate)로 주입하고,
 * 수신 측은 캐리어에서 컨텍스트를 복원해 consumer span 스코프 안에서 처리를 실행한다.
 * 전파 포맷은 Micrometer {@link Propagator}에 설정된 포맷(W3C 기본)을 그대로 따른다.
 */
@RequiredArgsConstructor
@Component
public class StreamTracePropagator {

    private final Tracer tracer;
    private final Propagator propagator;

    /**
     * 현재 트레이스 컨텍스트를 캐리어에 주입한다. 활성 스팬이 없으면 아무것도 추가하지 않는다.
     */
    public void injectCurrentContext(Map<String, String> carrier) {
        final Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return;
        }
        propagator.inject(currentSpan.context(), carrier, Map::put);
    }

    /**
     * 현재 트레이스 컨텍스트의 W3C traceparent 헤더 값을 반환한다. 활성 스팬이 없으면 null.
     */
    public String currentTraceparent() {
        final Map<String, String> carrier = new HashMap<>();
        injectCurrentContext(carrier);
        return carrier.get(StreamRecordFields.TRACEPARENT);
    }

    /**
     * 캐리어에서 트레이스 컨텍스트를 복원해 consumer span 스코프 안에서 task를 실행한다.
     * traceparent 필드가 없으면 스팬 없이 그대로 실행한다.
     */
    public void runInConsumerScope(Map<String, String> carrier, String spanName, Runnable task) {
        if (!carrier.containsKey(StreamRecordFields.TRACEPARENT)) {
            task.run();
            return;
        }
        final Span span = propagator.extract(carrier, Map::get)
                .name(spanName)
                .kind(Span.Kind.CONSUMER)
                .start();
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(span)) {
            task.run();
        } catch (Throwable t) {
            span.error(t);
            throw t;
        } finally {
            span.end();
        }
    }
}
