package coffeeshout.global.redis;

import coffeeshout.global.redis.stream.StreamConsumerRegister;
import coffeeshout.global.trace.Traceable;
import coffeeshout.global.trace.TracerProvider;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventHandlerExecutor {

    private final StreamConsumerRegister consumerRegistrar;
    private final TracerProvider tracerProvider;

    @SuppressWarnings("unchecked")
    public void handle(BaseEvent event) {
        try {
            final Consumer<BaseEvent> consumer = (Consumer<BaseEvent>) consumerRegistrar.getConsumer(event.getClass());
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
}
