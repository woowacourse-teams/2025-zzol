package coffeeshout.global.redis.stream;

import coffeeshout.global.config.properties.RedisStreamProperties;
import coffeeshout.global.redis.BaseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class StreamPublishManager {

    private final RedisStreamProperties properties;
    private final StreamPublisher streamPublisher;

    public void publish(String streamName, BaseEvent event) {
        String streamKey = properties.streams().stream()
                .filter(s -> s.name().equals(streamName))
                .findFirst()
                .map(RedisStreamProperties.StreamConfig::key)
                .orElseThrow(() -> new IllegalArgumentException("Unknown stream: " + streamName));

        streamPublisher.broadcast(event, streamKey);
    }
}
