package coffeeshout.global.redis.config;

import coffeeshout.global.redis.config.RedisStreamProperties.ThreadPoolConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamThreadPoolConfig {

    private final RedisStreamProperties properties;
    private static final String SHARED_NAME = "redis-stream-shared-%s-";

    @Bean
    public Map<String, Executor> streamSharedThreadPools() {
        final Map<String, Executor> sharedThreadPools = new HashMap<>();
        if (properties.threadPools() != null) {
            properties.threadPools().forEach((poolName, poolConfig) ->
                    sharedThreadPools.put(poolName, createThreadPoolExecutor(poolConfig, String.format(SHARED_NAME, poolName)))
            );
        }
        return sharedThreadPools;
    }

    public static ThreadPoolTaskExecutor createThreadPoolExecutor(ThreadPoolConfig config, String threadNamePrefix) {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.coreSize());
        executor.setMaxPoolSize(config.maxSize());
        executor.setQueueCapacity(config.queueCapacity());
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
