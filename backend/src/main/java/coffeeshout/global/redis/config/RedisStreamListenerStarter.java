package coffeeshout.global.redis.config;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.EventDispatcher;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@DependsOn("redisStreamThreadPoolConfig")
public class RedisStreamListenerStarter {

    public static final String STREAM_CONTAINER_BEAN_NAME_FORMAT = "stream-container-%s";
    
    private final RedisStreamProperties properties;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectMapper redisObjectMapper;
    private final EventDispatcher eventDispatcher;
    private final ApplicationContext applicationContext;
    private final GenericApplicationContext genericApplicationContext;

    public RedisStreamListenerStarter(
            RedisStreamProperties properties,
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper,
            EventDispatcher eventDispatcher,
            ApplicationContext applicationContext,
            GenericApplicationContext genericApplicationContext
    ) {
        this.properties = properties;
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisObjectMapper = redisObjectMapper;
        this.eventDispatcher = eventDispatcher;
        this.applicationContext = applicationContext;
        this.genericApplicationContext = genericApplicationContext;
    }

    @PostConstruct
    public void streamContainers() {
        if (properties.keys() == null) {
            log.warn("Redis Stream 설정이 없습니다. 리스너를 시작하지 않습니다.");
            return;
        }

        for (Map.Entry<String, StreamConfig> entry : properties.keys().entrySet()) {
            final String streamKey = entry.getKey();
            final StreamConfig streamConfig = entry.getValue();

            final StreamMessageListenerContainer<String, ObjectRecord<String, String>> container = createContainer(
                    redisConnectionFactory,
                    findExecutor(streamKey, streamConfig),
                    streamConfig
            );

            container.receive(StreamOffset.fromStart(streamKey), this::onMessage);

            genericApplicationContext.registerBean(
                    String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey),
                    StreamMessageListenerContainer.class,
                    () -> container
            );
        }
    }

    private Executor findExecutor(String streamKey, StreamConfig streamConfig) {
        if (streamConfig.isUseSharedThreadPool()) {
            return applicationContext.getBean(
                    RedisStreamThreadPoolConfig.convertBeanName(streamConfig.threadPoolName()),
                    Executor.class
            );
        }
        return applicationContext.getBean(RedisStreamThreadPoolConfig.convertBeanName(streamKey), Executor.class);
    }

    private void onMessage(ObjectRecord<String, String> message) {
        try {
            final BaseEvent event = redisObjectMapper.readValue(message.getValue(), BaseEvent.class);
            eventDispatcher.handle(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event: {}", message.getValue(), e);
        } catch (Exception e) {
            log.error("예외가 발생했습니다.", e);
            throw e;
        }
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
