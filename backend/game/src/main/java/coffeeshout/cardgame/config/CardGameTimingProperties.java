package coffeeshout.cardgame.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "card-game.timing")
public record CardGameTimingProperties(
        @NotNull @DurationMin(nanos = 1) Duration firstLoading,
        @NotNull @DurationMin(nanos = 1) Duration loading,
        @NotNull @DurationMin(nanos = 1) Duration prepare,
        @NotNull @DurationMin(nanos = 1) Duration playing,
        @NotNull @DurationMin(nanos = 1) Duration scoreBoard,
        @NotNull @DurationMin(nanos = 1) Duration earlyFinishDelay
) {
}
