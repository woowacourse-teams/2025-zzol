package coffeeshout.global.redis.config;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.EventHandlerExecutor;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamListenerStarter {

    private final RedisStreamProperties properties;
    private final RedisConnectionFactory redisConnectionFactory;

    @Qualifier("redisObjectMapper")
    private final ObjectMapper redisObjectMapper;
    private final EventHandlerExecutor eventHandlerExecutor;

    private final Map<String, Executor> streamSharedThreadPools;

    @PostConstruct
    public void streamContainers() {
        if (properties.keys() == null) {
            log.warn("Redis Stream 설정이 없습니다. 리스너를 시작하지 않습니다.");
            return;
        }

        for (Map.Entry<String, StreamConfig> entry : properties.keys().entrySet()) {
            final String streamKey = entry.getKey();
            final StreamConfig streamConfig = entry.getValue();

            final Executor executor = getExecutor(streamConfig, streamSharedThreadPools, streamKey);

            final StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                    createContainer(redisConnectionFactory, executor, streamConfig);

            container.receive(StreamOffset.fromStart(streamKey), this::onMessage);
        }
    }

    private void onMessage(ObjectRecord<String, String> message) {
        try {
            final BaseEvent event = redisObjectMapper.readValue(message.getValue(), BaseEvent.class);
            eventHandlerExecutor.handle(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event: {}", message.getValue(), e);
            throw new RuntimeException("Failed to parse event", e);
        }
    }

    private Executor getExecutor(StreamConfig streamConfig, Map<String, Executor> sharedThreadPools, String streamKey) {
        if (streamConfig.isUseSharedThreadPool()) {
            return Optional.of(sharedThreadPools.get(streamConfig.threadPoolName())).orElseThrow(
                    () -> new IllegalStateException("존재하지 않는 스레드풀 이름입니다: " + streamConfig.threadPoolName())
            );
        }
        return RedisStreamThreadPoolConfig.createThreadPoolExecutor(
                streamConfig.threadPool(),
                "redis-stream-" + streamKey.replace(":", "-") + "-"
        );
    }

    private StreamMessageListenerContainer<String, ObjectRecord<String, String>> createContainer(
            RedisConnectionFactory redisConnectionFactory,
            Executor executor,
            StreamConfig streamConfig
    ) {
        if (properties.commonSettings() == null) {
            throw new IllegalStateException("Redis Stream 공통 설정이 없습니다.");
        }

        final StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .batchSize(streamConfig.getBatchSize(properties.commonSettings()))
                        .executor(executor)
                        .pollTimeout(streamConfig.getPollTimeout(properties.commonSettings()))
                        .targetType(String.class)
                        .build();

        final StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        container.start();

        return container;
    }
}
