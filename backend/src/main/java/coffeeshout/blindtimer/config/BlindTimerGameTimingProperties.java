package coffeeshout.blindtimer.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "blind-timer.timing")
public record BlindTimerGameTimingProperties(
        @NotNull Duration description,
        @NotNull Duration prepare,
        @NotNull Duration blindDelay,
        @NotNull Duration timeoutBuffer,
        @NotNull Duration resultDelay
) {

    public BlindTimerGameTimingProperties {
        validatePositive(description, "description");
        validatePositive(prepare, "prepare");
        validatePositive(blindDelay, "blindDelay");
        validatePositive(timeoutBuffer, "timeoutBuffer");
        validatePositive(resultDelay, "resultDelay");
    }

    private static void validatePositive(Duration duration, String name) {
        if (duration != null && (duration.isZero() || duration.isNegative())) {
            throw new IllegalArgumentException(name + " 타이밍은 0보다 커야 합니다: " + duration);
        }
    }
}
