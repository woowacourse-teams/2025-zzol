package coffeeshout.global.config.redis;

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
        Config config = new Config();
        
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();
        String username = redisProperties.getUsername();
        boolean sslEnabled = redisProperties.getSsl().isEnabled();
        
        // SSL 여부에 따라 프로토콜 결정
        String protocol = sslEnabled ? "rediss://" : "redis://";
        
        var singleServerConfig = config.useSingleServer()
                .setAddress(protocol + host + ":" + port);
        
        // username 설정 (있으면)
        if (username != null && !username.isBlank()) {
            singleServerConfig.setUsername(username);
        }
        
        // password 설정 (있으면)
        if (password != null && !password.isBlank()) {
            singleServerConfig.setPassword(password);
        }
        
        return Redisson.create(config);
    }
}
