package coffeeshout.report.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack")
public record SlackProperties(
        String webhookUrl,
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
