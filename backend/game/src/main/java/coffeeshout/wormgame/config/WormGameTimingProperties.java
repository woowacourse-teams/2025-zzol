package coffeeshout.wormgame.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 단계 전이 타이밍. 틱 주기는 여기 없다 — 단일 출처는 {@code worm-game.rules.tick-millis}(도메인 규칙). */
@Validated
@ConfigurationProperties(prefix = "worm-game.timing")
public record WormGameTimingProperties(
        @NotNull @DurationMin(nanos = 1) Duration description,
        @NotNull @DurationMin(nanos = 1) Duration prepare,
        @NotNull @DurationMin(nanos = 1) Duration finish) {}
