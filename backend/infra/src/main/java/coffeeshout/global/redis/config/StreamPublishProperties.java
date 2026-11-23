package coffeeshout.global.redis.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Redis Stream 비동기 발행 설정.
 *
 * @param maxInFlight 응답을 기다리는 비동기 발행의 상한. 응답을 기다리지 않으면 스레드가 발행 수를 눌러 주지 않으므로,
 *                    Redis가 느려질 때 Lettuce 요청 큐(기본 상한 없음)에 명령이 끝없이 쌓이지 않도록 여기서 막는다.
 * @param drainTimeout 종료할 때 응답을 기다리는 비동기 발행이 끝나기를 기다리는 최대 시간. 커넥션을 닫기 전에
 *                     이미 넘긴 XADD가 끝나도록 해 종료 직전 이벤트 유실을 막는다.
 */
@Validated
@ConfigurationProperties(prefix = "redis.stream.publish")
public record StreamPublishProperties(
        @DefaultValue("10000") @Positive int maxInFlight,
        @DefaultValue("5s") @NotNull Duration drainTimeout) {}
