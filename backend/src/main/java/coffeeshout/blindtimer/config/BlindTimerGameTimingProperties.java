package coffeeshout.blindtimer.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "blind-timer.timing")
public record BlindTimerGameTimingProperties(
        @NotNull @DurationMin(nanos = 1) Duration description,
        @NotNull @DurationMin(nanos = 1) Duration prepare,
        @NotNull @DurationMin(nanos = 1) Duration blindDelay,
        @NotNull @DurationMin(nanos = 1) Duration timeoutBuffer,
        @NotNull @DurationMin(nanos = 1) Duration resultDelay
) {
}
