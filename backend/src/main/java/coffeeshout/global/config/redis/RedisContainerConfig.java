package coffeeshout.global.config.redis;

import coffeeshout.global.config.properties.RedisStreamProperties;
import coffeeshout.global.config.properties.RedisStreamProperties.StreamConfig;
import coffeeshout.global.config.properties.RedisStreamProperties.ThreadPoolConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
public class RedisContainerConfig {

    private final RedisStreamProperties properties;

    @Bean
    public Map<String, Executor> streamSharedThreadPools() {
        Map<String, Executor> executors = new HashMap<>();

        properties.threadPools().forEach((poolName, poolConfig) -> {
            ThreadPoolTaskExecutor executor = createThreadPoolExecutor(
                    poolConfig,
                    "redis-stream-shared-" + poolName + "-"
            );
            executors.put(poolName, executor);
        });

        return executors;
    }

    @Bean
    public Map<String, StreamMessageListenerContainer<String, ObjectRecord<String, String>>> streamContainers(
            RedisConnectionFactory redisConnectionFactory,
            Map<String, Executor> streamSharedThreadPools
    ) {
        Map<String, StreamMessageListenerContainer<String, ObjectRecord<String, String>>> containers = new HashMap<>();

        for (StreamConfig streamConfig : properties.streams()) {
            Executor executor = getOrCreateExecutor(streamConfig, streamSharedThreadPools);

            StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                    createContainer(redisConnectionFactory, executor);

            containers.put(streamConfig.name(), container);
        }

        return containers;
    }

    private Executor getOrCreateExecutor(
            StreamConfig streamConfig,
            Map<String, Executor> sharedThreadPools
    ) {
        if (streamConfig.threadPoolName() != null) {
            Executor executor = sharedThreadPools.get(streamConfig.threadPoolName());
            if (executor == null) {
                throw new IllegalStateException(
                        "ThreadPool not found: " + streamConfig.threadPoolName()
                );
            }
            return executor;
        }

        return createThreadPoolExecutor(
                streamConfig.threadPool(),
                "redis-stream-" + streamConfig.name() + "-"
        );
    }

    private ThreadPoolTaskExecutor createThreadPoolExecutor(
            ThreadPoolConfig config,
            String threadNamePrefix
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.coreSize());
        executor.setMaxPoolSize(config.maxSize());
        executor.setQueueCapacity(config.queueCapacity());
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    private StreamMessageListenerContainer<String, ObjectRecord<String, String>> createContainer(
            RedisConnectionFactory redisConnectionFactory,
            Executor executor
    ) {
        StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .batchSize(properties.batchSize())
                        .executor(executor)
                        .pollTimeout(properties.pollTimeout())
                        .targetType(String.class)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        container.start();
        return container;
    }
}
