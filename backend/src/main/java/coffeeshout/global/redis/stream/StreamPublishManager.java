package coffeeshout.global.redis.stream;

import coffeeshout.global.config.properties.RedisStreamProperties;
import coffeeshout.global.redis.BaseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class StreamPublishManager {

    private final RedisStreamProperties redisStreamProperties;
    private final StreamPublisher streamPublisher;

    public void publishCardGameChannel(BaseEvent event) {
        streamPublisher.broadcast(event, redisStreamProperties.cardGameSelectKey());
    }

    public void publishRoomChannel(BaseEvent event) {
        streamPublisher.broadcast(event, redisStreamProperties.roomJoinKey());
    }
}
