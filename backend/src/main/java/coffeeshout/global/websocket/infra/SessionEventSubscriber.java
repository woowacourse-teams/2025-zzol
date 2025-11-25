package coffeeshout.global.websocket.infra;

import coffeeshout.global.websocket.event.session.SessionBaseEvent;
import coffeeshout.global.websocket.event.session.SessionEventType;
import coffeeshout.global.websocket.event.session.SessionRegisteredEvent;
import coffeeshout.global.websocket.event.session.SessionRemovedEvent;
import coffeeshout.global.websocket.infra.handler.SessionEventHandler;
import coffeeshout.global.websocket.infra.handler.SessionEventHandlerFactory;
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
public class SessionEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ChannelTopic sessionEventTopic;
    private final SessionEventHandlerFactory handlerFactory;

    @PostConstruct
    public void subscribe() {
        redisMessageListenerContainer.addMessageListener(this, sessionEventTopic);
        log.info("세션 이벤트 구독 시작: topic={}", sessionEventTopic.getTopic());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            final String body = new String(message.getBody());
            final SessionEventType eventType = extractEventType(body);

            if (!handlerFactory.canHandle(eventType)) {
                log.warn("처리할 수 없는 세션 이벤트 타입: {}", eventType);
                return;
            }

            final SessionBaseEvent event = deserializeEvent(body, eventType);
            final SessionEventHandler<SessionBaseEvent> handler = handlerFactory.getHandler(eventType);
            handler.handle(event);

        } catch (Exception e) {
            log.error("세션 이벤트 처리 실패: message={}", new String(message.getBody()), e);
        }
    }

    private SessionEventType extractEventType(String body) throws Exception {
        final JsonNode jsonNode = objectMapper.readTree(body);
        final String eventTypeStr = jsonNode.get("eventType").asText();
        return SessionEventType.valueOf(eventTypeStr);
    }

    private SessionBaseEvent deserializeEvent(String body, SessionEventType eventType) throws Exception {
        return switch (eventType) {
            case SESSION_REGISTERED -> objectMapper.readValue(body, SessionRegisteredEvent.class);
            case SESSION_REMOVED -> objectMapper.readValue(body, SessionRemovedEvent.class);
        };
    }
}
