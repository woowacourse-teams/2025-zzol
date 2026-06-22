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
        @Positive long consumerQueueThreshold,
        @Positive long errorLogThreshold,
        @Positive long warnLogThreshold,
        @Positive int errorLogWindowMinutes,
        @Positive long http5xxThreshold,
        @Positive int cooldownMinutes
) {

    /**
     * 로그·5xx 집계가 공유하는 조회 윈도우.
     */
    public java.time.Duration window() {
        return java.time.Duration.ofMinutes(errorLogWindowMinutes);
    }
}
