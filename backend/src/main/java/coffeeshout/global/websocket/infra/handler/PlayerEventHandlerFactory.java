package coffeeshout.global.websocket.infra.handler;

import coffeeshout.global.websocket.event.player.PlayerBaseEvent;
import coffeeshout.global.websocket.event.player.PlayerEventType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlayerEventHandlerFactory {

    private final Map<PlayerEventType, PlayerEventHandler<? extends PlayerBaseEvent>> handlerMap;

    public PlayerEventHandlerFactory(List<PlayerEventHandler<? extends PlayerBaseEvent>> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        PlayerEventHandler::getSupportedEventType,
                        Function.identity()
                ));

        log.info("플레이어 이벤트 핸들러 팩토리 초기화: 핸들러 수={}, 지원 타입={}",
                handlers.size(), handlerMap.keySet());
    }

    @SuppressWarnings("unchecked")
    public <T extends PlayerBaseEvent> PlayerEventHandler<T> getHandler(PlayerEventType eventType) {
        final PlayerEventHandler<? extends PlayerBaseEvent> handler = handlerMap.get(eventType);

        if (handler == null) {
            throw new IllegalArgumentException("지원하지 않는 플레이어 이벤트 타입: " + eventType);
        }

        return (PlayerEventHandler<T>) handler;
    }

    public boolean canHandle(PlayerEventType eventType) {
        return handlerMap.containsKey(eventType);
    }
}
