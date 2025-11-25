package coffeeshout.room.infra.messaging;

import coffeeshout.room.domain.event.RoomBaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic roomEventTopic;

    public <T extends RoomBaseEvent> void publishEvent(T event) {
        try {
            redisTemplate.convertAndSend(roomEventTopic.getTopic(), event);
            log.info("이벤트 발행됨: eventType={}, eventId={}",
                    event.eventType(), event.eventId());
        } catch (Exception e) {
            log.error("이벤트 발행 실패: eventType={}, eventId={}",
                    event.eventType(), event.eventId(), e);
            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }
}
