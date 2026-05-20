package coffeeshout.redis;

import coffeeshout.metric.RedisStreamLatencyMetricService;
import coffeeshout.trace.Traceable;
import coffeeshout.trace.TracerProvider;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatcher {

    private final TracerProvider tracerProvider;
    private final ApplicationContext applicationContext;
    private final RedisStreamLatencyMetricService latencyMetricService;

    @SuppressWarnings("unchecked")
    public void handle(BaseEvent event) {
        try {
            recordLatency(event);

            final Consumer<BaseEvent> consumer = (Consumer<BaseEvent>) getConsumer(event.getClass());
            final Runnable handling = () -> consumer.accept(event);

            if (event instanceof Traceable traceable) {
                tracerProvider.executeWithTraceContext(traceable.traceInfo(), handling, event);
                return;
            }
            handling.run();

        } catch (Exception e) {
            log.error("이벤트 처리 실패: message={}", event, e);
        }
    }

    private void recordLatency(BaseEvent event) {
        try {
            latencyMetricService.recordLatency(event);
        } catch (Exception e) {
            log.warn("Redis Stream 지연 메트릭 기록 실패: eventId={}", event.eventId(), e);
        }
    }

    private <T extends BaseEvent> Consumer<T> getConsumer(Class<T> eventType) {
        final ResolvableType type = ResolvableType.forClassWithGenerics(Consumer.class, eventType);
        final ObjectProvider<Consumer<T>> provider = applicationContext.getBeanProvider(type);
        return provider.getObject();
    }
}
