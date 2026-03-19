package coffeeshout.bombrelay.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bomb-relay.timing")
public record BombRelayGameTimingProperties(
        @NotNull @DurationMin(nanos = 1) Duration description,
        @NotNull @DurationMin(nanos = 1) Duration prepare,
        @NotNull @DurationMin(nanos = 1) Duration minBombTimer,
        @NotNull @DurationMin(nanos = 1) Duration maxBombTimer,
        @NotNull @DurationMin(nanos = 1) Duration roundResultDelay,
        @NotNull @DurationMin(nanos = 1) Duration resultDelay
) {

    public BombRelayGameTimingProperties {
        if (minBombTimer != null && maxBombTimer != null
                && minBombTimer.compareTo(maxBombTimer) > 0) {
            throw new IllegalArgumentException(
                    "minBombTimer가 maxBombTimer보다 클 수 없습니다: min=" + minBombTimer + ", max=" + maxBombTimer);
        }
    }

    public Duration randomBombDuration() {
        final long minMs = minBombTimer.toMillis();
        final long maxMs = maxBombTimer.toMillis();
        final long randomMs = minMs + (long) (Math.random() * (maxMs - minMs));
        return Duration.ofMillis(randomMs);
    }
}
