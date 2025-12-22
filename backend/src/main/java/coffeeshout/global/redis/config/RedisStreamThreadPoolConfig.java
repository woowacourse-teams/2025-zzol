package coffeeshout.global.redis.config;

import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import coffeeshout.global.redis.config.RedisStreamProperties.ThreadPoolConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamThreadPoolConfig {

    private final RedisStreamProperties properties;
    private final GenericApplicationContext applicationContext;
    private static final String BEAN_NAME = "redis-stream-thread-pool-%s";

    @PostConstruct
    public void registerThreadPools() {
        properties.threadPools().forEach((poolName, poolConfig) -> applicationContext.registerBean(
                String.format(BEAN_NAME, poolName),
                ThreadPoolTaskExecutor.class,
                () -> createThreadPoolExecutor(poolConfig, String.format(BEAN_NAME, poolName))
        ));
        properties.keys().entrySet().stream()
                .filter(entry -> !entry.getValue().isUseSharedThreadPool())
                .forEach(entry -> {
                    final String keyName = entry.getKey();
                    final StreamConfig streamConfig = entry.getValue();
                    applicationContext.registerBean(
                            String.format(BEAN_NAME, keyName),
                            ThreadPoolTaskExecutor.class,
                            () -> createThreadPoolExecutor(streamConfig.threadPool(), String.format(BEAN_NAME, keyName))
                    );
                });
    }

    public static String convertBeanName(String poolName) {
        return String.format(BEAN_NAME, poolName);
    }

    public ThreadPoolTaskExecutor createThreadPoolExecutor(ThreadPoolConfig config, String threadNamePrefix) {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.coreSize());
        executor.setMaxPoolSize(config.maxSize());
        executor.setQueueCapacity(config.queueCapacity());
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
