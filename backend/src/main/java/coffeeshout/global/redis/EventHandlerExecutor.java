package coffeeshout.global.redis;

import coffeeshout.global.trace.Traceable;
import coffeeshout.global.trace.TracerProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventHandlerExecutor {

    private final EventHandlerMapping handlerFactory;
    private final TracerProvider tracerProvider;

    public void handle(BaseEvent event) {
        try {
            final EventHandler<BaseEvent> handler = handlerFactory.getHandler(event);
            final Runnable handling = () -> handler.handle(event);

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
