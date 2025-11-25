package coffeeshout.racinggame.infra.messaging;

import coffeeshout.racinggame.domain.event.RacingGameEventType;
import coffeeshout.racinggame.domain.event.StartRacingGameCommandEvent;
import coffeeshout.racinggame.domain.event.TapCommandEvent;
import coffeeshout.racinggame.infra.messaging.handler.RacingGameEventHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RacingGameEventSubscriber implements MessageListener {

    private final Map<RacingGameEventType, RacingGameEventHandler<?>> handlers;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ChannelTopic racingGameEventTopic;

    public RacingGameEventSubscriber(
            List<RacingGameEventHandler<?>> handlers,
            ObjectMapper objectMapper,
            RedisMessageListenerContainer redisMessageListenerContainer,
            ChannelTopic racingGameEventTopic
    ) {
        this.handlers = handlers.stream().collect(Collectors.toMap(
                RacingGameEventHandler::getSupportedEventType,
                handler -> handler
        ));
        this.objectMapper = objectMapper;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
        this.racingGameEventTopic = racingGameEventTopic;
    }

    @PostConstruct
    public void subscribe() {
        redisMessageListenerContainer.addMessageListener(this, racingGameEventTopic);
        log.info("레이싱 게임 이벤트 구독 시작: topic={}", racingGameEventTopic.getTopic());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            final String body = new String(message.getBody());
            final RacingGameEventType eventType = extractEventType(body);

            if (!canHandle(eventType)) {
                log.warn("처리할 수 없는 이벤트 타입: {}", eventType);
                return;
            }

            final Object event = deserializeEvent(body, eventType);
            @SuppressWarnings("unchecked")
            final RacingGameEventHandler<Object> handler = (RacingGameEventHandler<Object>) handlers.get(eventType);
            handler.handle(event);

        } catch (Exception e) {
            log.error("레이싱 게임 이벤트 처리 실패: message={}", new String(message.getBody()), e);
        }
    }

    private RacingGameEventType extractEventType(String body) throws Exception {
        final JsonNode jsonNode = objectMapper.readTree(body);
        final String eventTypeStr = jsonNode.get("eventType").asText();
        return RacingGameEventType.valueOf(eventTypeStr);
    }

    private boolean canHandle(RacingGameEventType eventType) {
        return handlers.containsKey(eventType);
    }

    private Object deserializeEvent(String body, RacingGameEventType eventType) throws Exception {
        return switch (eventType) {
            case START_RACING_GAME_COMMAND -> objectMapper.readValue(body, StartRacingGameCommandEvent.class);
            case TAP_COMMAND -> objectMapper.readValue(body, TapCommandEvent.class);
        };
    }
}
