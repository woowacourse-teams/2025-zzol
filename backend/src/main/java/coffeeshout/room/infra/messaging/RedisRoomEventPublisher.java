package coffeeshout.room.infra.messaging;

import coffeeshout.global.config.redis.EventTopicRegistry;
import coffeeshout.room.application.port.RoomEventPublisher;
import coffeeshout.room.domain.event.RoomBaseEvent;
import coffeeshout.room.domain.event.RoomJoinEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRoomEventPublisher implements RoomEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RoomEnterStreamProducer roomEnterStreamProducer;

    @Override
    public <T extends RoomBaseEvent> void publish(T event) {
        try {
            // 이벤트 타입에 따라 발행 메커니즘 선택
            // TODO: 이벤트 타입이 증가하면 전략 패턴으로 리팩토링 고려
            if (event instanceof RoomJoinEvent roomJoinEvent) {
                publishToStream(roomJoinEvent);
            } else {
                publishToPubSub(event);
            }

            log.info("이벤트 발행 완료: eventType={}, eventId={}",
                    event.eventType(), event.eventId());

        } catch (Exception e) {
            log.error("이벤트 발행 실패: eventType={}, eventId={}",
                    event.eventType(), event.eventId(), e);
            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }

    private void publishToPubSub(RoomBaseEvent event) {
        String topic = EventTopicRegistry.ROOM.getTopic();
        redisTemplate.convertAndSend(topic, event);

        log.debug("Redis Pub/Sub로 이벤트 발행: topic={}, eventType={}",
                topic, event.eventType());
    }

    private void publishToStream(RoomJoinEvent event) {
        roomEnterStreamProducer.broadcastEnterRoom(event);
        log.debug("방 입장 이벤트 Stream 발행 완료: eventId={}, joinCode={}, guestName={}",
                event.eventId(), event.joinCode(), event.guestName());
    }
}
