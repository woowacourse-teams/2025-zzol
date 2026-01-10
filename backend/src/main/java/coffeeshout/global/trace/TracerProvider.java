package coffeeshout.global.trace;

import coffeeshout.global.redis.BaseEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TracerProvider {

    private final Tracer tracer;

    public void executeWithTraceContext(TraceInfo traceInfo, Runnable task, BaseEvent event) {
        if (!traceInfo.traceable()) {
            task.run();
            return;
        }
        final TraceContext context = tracer.traceContextBuilder()
                .traceId(traceInfo.traceId())
                .spanId(traceInfo.spanId())
                .sampled(true)
                .build();
        final Span span = tracer.spanBuilder()
                .name(event.getClass().getSimpleName())
                .setParent(context)
                .start();
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(span)) {
            task.run();
        } finally {
            span.end();
        }
    }
}
