package coffeeshout.zzolbot.monitor.infra;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * zzolbot 모니터링 알림용 Slack 설정. admin 모듈의 SlackProperties와 분리해 모듈 의존을 끊는다.
 */
@ConfigurationProperties(prefix = "zzol-bot.slack")
public record ZzolBotSlackProperties(
        String webhookUrl,
        Duration connectTimeout,
        Duration readTimeout
) {

    public ZzolBotSlackProperties {
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(3);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(5);
        }
    }

    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }
}
