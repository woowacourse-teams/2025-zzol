package coffeeshout.speedtouch.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "speed-touch.timing")
public record SpeedTouchGameTimingProperties(
        @NotNull Duration description,
        @NotNull Duration prepare,
        @NotNull Duration playing
) {

    public SpeedTouchGameTimingProperties {
        validatePositive(description, "description");
        validatePositive(prepare, "prepare");
        validatePositive(playing, "playing");
    }

    private static void validatePositive(Duration duration, String name) {
        if (duration != null && (duration.isZero() || duration.isNegative())) {
            throw new IllegalArgumentException(name + " 타이밍은 0보다 커야 합니다: " + duration);
        }
    }
}
