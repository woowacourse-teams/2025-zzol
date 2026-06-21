package coffeeshout.zzolbot.monitor.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 능동 모니터링 설정. 수집 주기·임계값·재알림 쿨다운을 외부화한다.
 */
@Validated
@ConfigurationProperties(prefix = "zzol-bot.monitor")
public record MonitorProperties(
        boolean enabled,
        @NotBlank String cron,
        @Positive long deadLetterThreshold,
        @Positive long streamBacklogThreshold,
        @Positive long errorLogThreshold,
        @Positive int errorLogWindowMinutes,
        @Positive int cooldownMinutes
) {

    public java.time.Duration errorLogWindow() {
        return java.time.Duration.ofMinutes(errorLogWindowMinutes);
    }
}
