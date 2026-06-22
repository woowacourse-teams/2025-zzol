package coffeeshout.zzolbot.monitor.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 모니터링 설정. 보강 활성 여부와 ERROR 로그 조회 윈도우를 외부화한다.
 */
@Validated
@ConfigurationProperties(prefix = "zzol-bot.monitor")
public record MonitorProperties(
        boolean enabled,
        @Positive int errorLogWindowMinutes
) {

    /**
     * ERROR 로그 샘플 조회 윈도우.
     */
    public java.time.Duration window() {
        return java.time.Duration.ofMinutes(errorLogWindowMinutes);
    }
}
