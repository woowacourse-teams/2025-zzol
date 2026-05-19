package coffeeshout.blockstacking.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "block-stacking.timing")
public record BlockStackingTimingProperties(
        @NotNull @DurationMin(nanos = 1) Duration prepare,
        @NotNull @DurationMin(nanos = 1) Duration playing,
        @NotNull @DurationMin(nanos = 1) Duration allFailedDelay
) {
}
