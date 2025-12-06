package coffeeshout.minigame.infra.messaging;

import coffeeshout.global.config.redis.EventTopicRegistry;
import coffeeshout.minigame.event.MiniGameBaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public <T extends MiniGameBaseEvent> void publishEvent(T event) {
        try {
            final String topic = EventTopicRegistry.MINI_GAME.getTopic();
            redisTemplate.convertAndSend(topic, event);
            log.info("미니게임 이벤트 발행됨: eventType={}, eventId={}",
                    event.eventType(), event.eventId());
        } catch (Exception e) {
            log.error("미니게임 이벤트 발행 실패: eventType={}, eventId={}",
                    event.eventType(), event.eventId(), e);
            throw new RuntimeException("미니게임 이벤트 발행 실패", e);
        }
    }
}
