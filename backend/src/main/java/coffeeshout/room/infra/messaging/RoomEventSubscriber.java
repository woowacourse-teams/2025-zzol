package coffeeshout.room.infra.messaging;

import coffeeshout.global.trace.Traceable;
import coffeeshout.global.trace.TracerProvider;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import coffeeshout.room.domain.event.PlayerKickEvent;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import coffeeshout.room.domain.event.RoomBaseEvent;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.event.RoomEventType;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.event.RouletteShowEvent;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.infra.messaging.handler.RoomEventHandler;
import coffeeshout.room.infra.messaging.handler.RoomEventHandlerFactory;
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
public class RoomEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ChannelTopic roomEventTopic;
    private final RoomEventHandlerFactory handlerFactory;
    private final TracerProvider tracerProvider;

    @PostConstruct
    public void subscribe() {
        redisMessageListenerContainer.addMessageListener(this, roomEventTopic);
        log.info("방 이벤트 구독 시작: topic={}", roomEventTopic.getTopic());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            final String body = new String(message.getBody());
            final RoomEventType eventType = extractEventType(body);

            if (!handlerFactory.canHandle(eventType)) {
                log.warn("처리할 수 없는 이벤트 타입: {}", eventType);
                return;
            }

            final RoomBaseEvent event = deserializeEvent(body, eventType);
            final RoomEventHandler<RoomBaseEvent> handler = handlerFactory.getHandler(eventType);
            if (event instanceof Traceable traceable) {
                tracerProvider.executeWithTraceContext(
                        traceable.getTraceInfo(),
                        () -> handler.handle(event),
                        event.eventType().name()
                );
                return;
            }
            handler.handle(event);

        } catch (Exception e) {
            log.error("이벤트 처리 실패: message={}", new String(message.getBody()), e);
        }
    }

    private RoomEventType extractEventType(String body) throws Exception {
        final JsonNode jsonNode = objectMapper.readTree(body);
        final String eventTypeStr = jsonNode.get("eventType").asText();
        return RoomEventType.valueOf(eventTypeStr);
    }

    private RoomBaseEvent deserializeEvent(String body, RoomEventType eventType) throws Exception {
        return switch (eventType) {
            case ROOM_CREATE -> objectMapper.readValue(body, RoomCreateEvent.class);
            case ROOM_JOIN -> objectMapper.readValue(body, RoomJoinEvent.class);
            case PLAYER_LIST_UPDATE -> objectMapper.readValue(body, PlayerListUpdateEvent.class);
            case PLAYER_READY -> objectMapper.readValue(body, PlayerReadyEvent.class);
            case PLAYER_KICK -> objectMapper.readValue(body, PlayerKickEvent.class);
            case MINI_GAME_SELECT -> objectMapper.readValue(body, MiniGameSelectEvent.class);
            case ROULETTE_SHOW -> objectMapper.readValue(body, RouletteShowEvent.class);
            case ROULETTE_SPIN -> objectMapper.readValue(body, RouletteSpinEvent.class);
            case QR_CODE_COMPLETE -> objectMapper.readValue(body, QrCodeStatusEvent.class);
        };
    }
}
