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

    public void publish(String channelName, BaseEvent event) {
        String streamKey = properties.channels().stream()
                .filter(c -> c.name().equals(channelName))
                .findFirst()
                .map(RedisStreamProperties.ChannelConfig::key)
                .orElseThrow(() -> new IllegalArgumentException("Unknown channel: " + channelName));

        streamPublisher.broadcast(event, streamKey);
    }
}
