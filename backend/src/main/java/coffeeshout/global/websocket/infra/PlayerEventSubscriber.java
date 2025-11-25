package coffeeshout.global.websocket.infra;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.global.websocket.event.player.PlayerBaseEvent;
import coffeeshout.global.websocket.event.player.PlayerDisconnectedEvent;
import coffeeshout.global.websocket.event.player.PlayerEventType;
import coffeeshout.global.websocket.event.player.PlayerReconnectedEvent;
import coffeeshout.global.websocket.infra.handler.PlayerEventHandler;
import coffeeshout.global.websocket.infra.handler.PlayerEventHandlerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ChannelTopic playerEventTopic;
    private final PlayerEventHandlerFactory handlerFactory;

    @PostConstruct
    public void subscribe() {
        redisMessageListenerContainer.addMessageListener(this, playerEventTopic);
        log.info("플레이어 이벤트 구독 시작: topic={}", playerEventTopic.getTopic());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        final String body = new String(message.getBody());

        try {
            final PlayerEventType eventType = extractEventType(body);

            if (!handlerFactory.canHandle(eventType)) {
                log.warn("처리할 수 없는 플레이어 이벤트 타입: {}", eventType);
                return;
            }

            final PlayerBaseEvent event = deserializeEvent(body, eventType);
            final PlayerEventHandler<PlayerBaseEvent> handler = handlerFactory.getHandler(eventType);
            handler.handle(event);

        } catch (InvalidStateException | InvalidArgumentException e) {
            log.warn("플레이어 이벤트 처리 중 오류: message={}", body, e);
        } catch (Exception e) {
            log.error("플레이어 이벤트 처리 실패: message={}", body, e);
        }
    }

    private PlayerEventType extractEventType(String body) throws Exception {
        final JsonNode jsonNode = objectMapper.readTree(body);
        final String eventTypeStr = jsonNode.get("eventType").asText();
        return PlayerEventType.valueOf(eventTypeStr);
    }

    private PlayerBaseEvent deserializeEvent(String body, PlayerEventType eventType) throws Exception {
        return switch (eventType) {
            case PLAYER_DISCONNECTED -> objectMapper.readValue(body, PlayerDisconnectedEvent.class);
            case PLAYER_RECONNECTED -> objectMapper.readValue(body, PlayerReconnectedEvent.class);
        };
    }
}
