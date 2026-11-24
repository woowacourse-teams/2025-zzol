package coffeeshout.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.redis.config.RedisProperties.Ssl;
import io.lettuce.core.ClientOptions;
import org.junit.jupiter.api.Test;

class RedisConfigTest {

    private final ClientOptions clientOptions = new RedisConfig(new RedisProperties("localhost", 6379, new Ssl(false)))
            .redisConnectionFactory()
            .getClientConfiguration()
            .getClientOptions()
            .orElseThrow();

    @Test
    void 커넥션마다_응답을_기다리는_명령_수에_상한을_둔다() {
        // 비동기 발행은 스레드가 발행 수를 눌러 주지 않아, Redis가 느려지면 이 상한이 명령이 쌓이는 것을 막는다
        assertThat(clientOptions.getRequestQueueSize()).isEqualTo(RedisConfig.REQUEST_QUEUE_SIZE);
    }

    @Test
    void 클라이언트_옵션을_직접_넣어도_명령_타임아웃은_켜져_있다() {
        // 직접 넣은 옵션이 Spring Data Redis 기본 옵션을 대신한다. 응답이 오지 않는 명령이 영원히 남지 않도록
        // 타임아웃이 켜져 있는지 본다
        assertThat(clientOptions.getTimeoutOptions().isTimeoutCommands()).isTrue();
    }
}
