package coffeeshout.global.redis.config;

import io.lettuce.core.ClientOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    /**
     * 커넥션마다 응답을 기다리는 명령 수의 상한. 넘으면 Lettuce가 새 명령을 보내지 않고 바로 실패시킨다.
     * <p>
     * 비동기 발행은 호출 스레드가 응답을 기다리지 않아, 스레드 수가 더는 발행 수를 눌러 주지 않는다. 상한이 없으면
     * Redis가 느려지거나 끊겼을 때 명령이 메모리에 끝없이 쌓인다 (ADR-0039).
     */
    static final int REQUEST_QUEUE_SIZE = 10_000;

    private final RedisProperties redisProperties;

    /**
     * 반환 타입을 {@link LettuceConnectionFactory}로 선언해 {@link ReactiveRedisConnectionFactory}로도 매칭되게 한다.
     * 그래야 Spring Boot 자동 구성이 비동기 발행에 쓰는 {@code ReactiveStringRedisTemplate}을 만든다.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        final RedisStandaloneConfiguration redisStandaloneConfiguration =
                new RedisStandaloneConfiguration(redisProperties.host(), redisProperties.port());

        final LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig =
                LettuceClientConfiguration.builder()
                        .clientOptions(ClientOptions.builder()
                                .requestQueueSize(REQUEST_QUEUE_SIZE)
                                .build());

        if (redisProperties.ssl().enabled()) {
            clientConfig.useSsl();
        }

        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig.build());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        final RedisSerializer<Object> jsonRedisSerializer = RedisSerializer.json();
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
