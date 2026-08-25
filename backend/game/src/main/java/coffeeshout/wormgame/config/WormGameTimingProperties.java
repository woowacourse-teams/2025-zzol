package coffeeshout.wormgame.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 단계 전이 타이밍. 틱 주기는 {@code tick}이고, 도메인 규칙의 {@code tickMillis}와 같은 값이어야
 * 점수(생존 ms)가 실시간과 일치한다.
 */
@Validated
@ConfigurationProperties(prefix = "worm-game.timing")
public record WormGameTimingProperties(
        @NotNull @DurationMin(nanos = 1) Duration description,
        @NotNull @DurationMin(nanos = 1) Duration prepare,
        @NotNull @DurationMin(nanos = 1) Duration tick,
        @NotNull @DurationMin(nanos = 1) Duration finish,
        @NotNull @DurationMin(nanos = 1) Duration snapshotInterval) {}
