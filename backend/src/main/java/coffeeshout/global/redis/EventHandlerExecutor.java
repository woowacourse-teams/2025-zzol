package coffeeshout.global.redis;

import coffeeshout.global.trace.Traceable;
import coffeeshout.global.trace.TracerProvider;
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
public class EventHandlerExecutor {

    private final TracerProvider tracerProvider;
    private final ApplicationContext applicationContext;

    @SuppressWarnings("unchecked")
    public void handle(BaseEvent event) {
        try {
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

    private <T extends BaseEvent> Consumer<T> getConsumer(Class<T> eventType) {
        final ResolvableType type = ResolvableType.forClassWithGenerics(Consumer.class, eventType);
        final ObjectProvider<Consumer<T>> provider = applicationContext.getBeanProvider(type);
        return provider.getObject();
    }
}
