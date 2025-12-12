package coffeeshout.global.redis.pubsub;

import coffeeshout.global.redis.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PubSubEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publishEvent(BaseEvent event, ChannelTopic channelTopic) {
        try {
            redisTemplate.convertAndSend(channelTopic.getTopic(), event);
            log.info("이벤트 발행됨: eventType={}, eventId={}", event.getClass().getSimpleName(), event.eventId());
        } catch (Exception e) {
            log.error("이벤트 발행 실패: eventType={}, eventId={}", event.getClass().getSimpleName(), event.eventId(), e);
            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }
}
