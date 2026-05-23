package coffeeshout.report.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "slack")
public record SlackProperties(
        @NotNull String webhookUrl,
        Duration connectTimeout,
        Duration readTimeout
) {

    public SlackProperties {
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(3);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(5);
    }

    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }
}
