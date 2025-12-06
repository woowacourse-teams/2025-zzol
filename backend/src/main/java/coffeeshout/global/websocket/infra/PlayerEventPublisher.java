package coffeeshout.global.websocket.infra;

import coffeeshout.global.config.redis.EventTopicRegistry;
import coffeeshout.global.websocket.event.player.PlayerBaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public <T extends PlayerBaseEvent> void publishEvent(T event) {
        try {
            final String topic = EventTopicRegistry.PLAYER.getTopic();
            redisTemplate.convertAndSend(topic, event);
            log.info("플레이어 이벤트 발행됨: eventType={}, eventId={}",
                    event.eventType(), event.eventId());
        } catch (Exception e) {
            log.error("플레이어 이벤트 발행 실패: eventType={}, eventId={}",
                    event.eventType(), event.eventId(), e);
            throw new RuntimeException("플레이어 이벤트 발행 실패", e);
        }
    }
}
