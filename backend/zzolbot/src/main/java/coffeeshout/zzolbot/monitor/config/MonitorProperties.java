package coffeeshout.zzolbot.monitor.config;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 모니터링 설정. 분석 활성 여부, ERROR 로그 조회 윈도우, 재분석 간격을 외부화한다.
 */
@Validated
@ConfigurationProperties(prefix = "zzol-bot.monitor")
public record MonitorProperties(
        boolean enabled,
        @Positive int errorLogWindowMinutes,
        @PositiveOrZero int enrichCooldownMinutes
) {

    /**
     * ERROR 로그 샘플 조회 윈도우.
     */
    public Duration window() {
        return Duration.ofMinutes(errorLogWindowMinutes);
    }

    /**
     * 같은 fingerprint를 이 시간 안에는 다시 분석하지 않는다(지문별 중복 분석 방지). 웹훅 재시도·flapping을
     * 흡수하고, 지속되는 장애의 LLM 재호출을 fingerprint당 일정 시간에 한 번으로 묶어 비용을 통제한다. 0이면 비활성.
     */
    public Duration enrichCooldown() {
        return Duration.ofMinutes(enrichCooldownMinutes);
    }
}
