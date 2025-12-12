package coffeeshout.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventHandlerMapping {

    private final Map<Class<? extends BaseEvent>, EventHandler<? extends BaseEvent>> handlerMap = new HashMap<>();


    public EventHandlerMapping(
            List<EventHandler<? extends BaseEvent>> handlers,
            @Qualifier("redisObjectMapper") ObjectMapper mapper
    ) {
        handlers.forEach(handler -> {
            handlerMap.put(handler.eventType(), handler);
            Class<? extends BaseEvent> eventType = handler.eventType();
            mapper.registerSubtypes(new NamedType(eventType, eventType.getSimpleName()));
        });
    }

    public <T extends BaseEvent> EventHandler<T> getHandler(T event) {
        if (!handlerMap.containsKey(event.getClass())) {
            throw new IllegalArgumentException("지원하지 않는 이벤트 타입: " + event.getClass());
        }
        //noinspection unchecked
        return (EventHandler<T>) handlerMap.get(event.getClass());
    }
}
