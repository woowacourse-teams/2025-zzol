package coffeeshout.zzolbot.remediation.config;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 자동 수정 봇 설정. 활성 여부, GitHub repository_dispatch 대상·토큰, 일일 PR 디스패치 하드캡,
 * 같은 fingerprint 재수정 쿨다운, 자동 수정 대상 결함 화이트리스트를 외부화한다.
 */
@Validated
@ConfigurationProperties(prefix = "zzol-bot.remediation")
public record RemediationProperties(
        boolean enabled,
        String githubToken,
        String repoOwner,
        String repoName,
        @Positive long dailyMax,
        @PositiveOrZero int cooldownMinutes,
        @NotNull List<String> defectWhitelist
) {

    public RemediationProperties {
        defectWhitelist = defectWhitelist != null ? List.copyOf(defectWhitelist) : List.of();
    }

    /**
     * 같은 fingerprint를 이 시간 안에는 다시 수정 시도하지 않는다(동일 장애 PR 폭주 방지). 0이면 비활성.
     */
    public Duration cooldown() {
        return Duration.ofMinutes(cooldownMinutes);
    }

    /**
     * 결함 유형이 자동 수정 대상(화이트리스트)인지. 화이트리스트 밖이면 디스패치하지 않고 제안에 머문다.
     */
    public boolean isWhitelisted(DefectType type) {
        return type != null && defectWhitelist.contains(type.name());
    }
}
