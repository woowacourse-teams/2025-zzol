package coffeeshout.global.websocket.infra;

import coffeeshout.global.websocket.event.session.SessionBaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic sessionEventTopic;

    public <T extends SessionBaseEvent> void publishEvent(T event) {
        try {
            redisTemplate.convertAndSend(sessionEventTopic.getTopic(), event);
            log.info("세션 이벤트 발행됨: eventType={}, eventId={}",
                    event.eventType(), event.eventId());
        } catch (Exception e) {
            log.error("세션 이벤트 발행 실패: eventType={}, eventId={}",
                    event.eventType(), event.eventId(), e);
            throw new RuntimeException("세션 이벤트 발행 실패", e);
        }
    }
}
