package coffeeshout.global.redis.stream;

import coffeeshout.global.config.properties.RedisStreamProperties;
import coffeeshout.global.config.properties.RedisStreamProperties.ChannelConfig;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.EventHandlerExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicStreamListenerRegistrar {

    private final RedisStreamProperties properties;
    private final Map<String, StreamMessageListenerContainer<String, ObjectRecord<String, String>>> streamContainers;

    @Qualifier("redisObjectMapper")
    private final ObjectMapper redisObjectMapper;
    private final EventHandlerExecutor eventHandlerExecutor;

    @PostConstruct
    public void registerListeners() {
        for (ChannelConfig channelConfig : properties.channels()) {
            final StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                    streamContainers.get(channelConfig.name());

            if (container == null) {
                throw new IllegalStateException("Container not found for channel: " + channelConfig.name());
            }

            container.receive(
                    StreamOffset.fromStart(channelConfig.key()),
                    this::onMessage
            );

            log.info("Registered listener for channel: {} (key: {})", channelConfig.name(), channelConfig.key());
        }
    }

    private void onMessage(ObjectRecord<String, String> message) {
        try {
            BaseEvent event = redisObjectMapper.readValue(message.getValue(), BaseEvent.class);
            eventHandlerExecutor.handle(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event: {}", message.getValue(), e);
            throw new RuntimeException("Failed to parse event", e);
        }
    }
}
