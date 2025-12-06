package coffeeshout.racinggame.infra.messaging;

import coffeeshout.global.config.redis.EventTopicRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RacingGameEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public <T> void publishEvent(T event) {
        try {
            final String topic = EventTopicRegistry.RACING_GAME.getTopic();
            redisTemplate.convertAndSend(topic, event);
            log.info("레이싱 게임 이벤트 발행됨: event={}", event);
        } catch (Exception e) {
            log.error("레이싱 게임 이벤트 발행 실패: event={}", event, e);
            throw new RuntimeException("레이싱 게임 이벤트 발행 실패", e);
        }
    }
}
