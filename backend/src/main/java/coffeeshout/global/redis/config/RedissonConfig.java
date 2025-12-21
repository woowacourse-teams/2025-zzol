package coffeeshout.global.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    @ConditionalOnProperty(name = "redisson.enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        final Config config = new Config();
        
        final String host = redisProperties.getHost();
        final int port = redisProperties.getPort();
        final String password = redisProperties.getPassword();
        final String username = redisProperties.getUsername();
        final boolean sslEnabled = redisProperties.getSsl().isEnabled();
        
        final String protocol = sslEnabled ? "rediss://" : "redis://";
        
        final var singleServerConfig = config.useSingleServer()
                .setAddress(protocol + host + ":" + port);
        
        if (username != null && !username.isBlank()) {
            singleServerConfig.setUsername(username);
        }
        
        if (password != null && !password.isBlank()) {
            singleServerConfig.setPassword(password);
        }
        
        return Redisson.create(config);
    }
}
