package coffeeshout.numberpoker.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "number-poker.timing")
public record NumberPokerTimingProperties(
        @NotNull @DurationMin(nanos = 1) Duration stage1,
        @NotNull @DurationMin(nanos = 1) Duration stage2,
        @NotNull @DurationMin(nanos = 1) Duration roundReady
) {
}
