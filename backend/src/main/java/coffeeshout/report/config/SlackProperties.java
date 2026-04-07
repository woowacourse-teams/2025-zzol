package coffeeshout.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack")
public record SlackProperties(String webhookUrl) {

    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }
}
