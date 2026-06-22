package coffeeshout.zzolbot.monitor.config;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 모니터링 설정. 보강 활성 여부, ERROR 로그 조회 윈도우, 중복 억제 윈도우를 외부화한다.
 */
@Validated
@ConfigurationProperties(prefix = "zzol-bot.monitor")
public record MonitorProperties(
        boolean enabled,
        @Positive int errorLogWindowMinutes,
        @PositiveOrZero int duplicateSuppressionSeconds
) {

    /**
     * ERROR 로그 샘플 조회 윈도우.
     */
    public Duration window() {
        return Duration.ofMinutes(errorLogWindowMinutes);
    }

    /**
     * 같은 fingerprint의 firing 재배달(웹훅 재시도·재시작)을 멱등 처리하기 위한 중복 억제 윈도우.
     * Alertmanager {@code repeat_interval}보다 짧게 둬야 의도된 주기적 재알림은 통과한다. 0이면 가드 비활성.
     */
    public Duration duplicateSuppressionWindow() {
        return Duration.ofSeconds(duplicateSuppressionSeconds);
    }
}
