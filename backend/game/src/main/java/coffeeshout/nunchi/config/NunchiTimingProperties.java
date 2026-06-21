package coffeeshout.nunchi.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 눈치게임 타이밍 외부 설정(ADR-0031 결정 7). 동시 윈도우·충돌 쿨다운·무입력 idle·하드 캡을
 * 코드 상수가 아니라 {@code nunchi.timing.*}로 분리해 환경별 조정을 가능하게 한다.
 */
@Validated
@ConfigurationProperties(prefix = "nunchi.timing")
public record NunchiTimingProperties(
        @NotNull @DurationMin(nanos = 1) Duration numberWindow,
        @NotNull @DurationMin(nanos = 1) Duration collisionCooldown,
        @NotNull @DurationMin(nanos = 1) Duration idleTimeout,
        @NotNull @DurationMin(nanos = 1) Duration hardCap,
        @NotNull @DurationMin(nanos = 1) Duration prepare,
        @NotNull @DurationMin(nanos = 1) Duration earlyFinishDelay
) {
}
