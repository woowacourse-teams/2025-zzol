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
}
