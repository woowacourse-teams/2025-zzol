package coffeeshout.bombrelay.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bomb-relay.timing")
public record BombRelayGameTimingProperties(
        @NotNull Duration description,
        @NotNull Duration prepare,
        @NotNull Duration minBombTimer,
        @NotNull Duration maxBombTimer,
        @NotNull Duration roundResultDelay,
        @NotNull Duration resultDelay
) {

    public BombRelayGameTimingProperties {
        validatePositive(description, "description");
        validatePositive(prepare, "prepare");
        validatePositive(minBombTimer, "minBombTimer");
        validatePositive(maxBombTimer, "maxBombTimer");
        validatePositive(roundResultDelay, "roundResultDelay");
        validatePositive(resultDelay, "resultDelay");
        if (minBombTimer != null && maxBombTimer != null
                && minBombTimer.compareTo(maxBombTimer) > 0) {
            throw new IllegalArgumentException(
                    "minBombTimer가 maxBombTimer보다 클 수 없습니다: min=" + minBombTimer + ", max=" + maxBombTimer);
        }
    }

    private static void validatePositive(Duration duration, String name) {
        if (duration != null && (duration.isZero() || duration.isNegative())) {
            throw new IllegalArgumentException(name + " 타이밍은 0보다 커야 합니다: " + duration);
        }
    }

    public Duration randomBombDuration() {
        final long minMs = minBombTimer.toMillis();
        final long maxMs = maxBombTimer.toMillis();
        final long randomMs = minMs + (long) (Math.random() * (maxMs - minMs));
        return Duration.ofMillis(randomMs);
    }
}
