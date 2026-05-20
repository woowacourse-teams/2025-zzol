package coffeeshout.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redisson")
public record RedissonProperties(
        boolean enabled,
        int timeout,
        int connectTimeout,
        int retryAttempts,
        int retryInterval
) {
    public RedissonProperties {
        if (timeout <= 0) timeout = 1000;
        if (connectTimeout <= 0) connectTimeout = 1000;
        if (retryAttempts < 0) retryAttempts = 1;
        if (retryInterval <= 0) retryInterval = 500;
    }
}
