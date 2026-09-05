package coffeeshout.profanity.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * @param requestTimeout Gemini HTTP 요청 타임아웃. 없으면 응답이 끝내 오지 않는 호출 하나가 회차 스레드를
 *                       무기한 점유한다. {@code batchSize}를 키우면 응답 생성 시간도 늘어나므로 함께 올려야 한다.
 *                       하한을 두는 이유는 SDK가 이 값을 OkHttp {@code callTimeout}에 그대로 넣고, OkHttp에서
 *                       0은 무제한이라서다. {@code @Positive}는 {@link Duration}에 적용되지 않는다.
 * @param maxRunDuration 한 회차가 실행기 스레드를 붙잡아도 되는 상한. 넘기면 남은 적체는 다음 회차로 넘긴다.
 *                       {@code ProfanityAuditService.LOCK_LEASE_MILLIS}보다 반드시 짧아야 한다.
 * @param maxAttempts    한 행이 검열 호출 실패를 견디는 횟수. 여기 닿으면 DEAD_LETTER가 되어 UNAUDITED 스캔에서
 *                       빠진다. 늘리면 되풀이 실패하는 행 하나가 회차마다 Gemini 호출을 그만큼 더 태운다.
 */
@Validated
@ConfigurationProperties(prefix = "nickname-audit")
public record NicknameAuditProperties(
        String geminiApiKey,
        @NotBlank String model,
        @DecimalMin("0.0") @DecimalMax("1.0") double flaggedThreshold,
        @Positive int batchSize,
        @Positive int feedbackInjectionThreshold,
        @DefaultValue("2") @Positive int minTermLength,

        @DefaultValue("120s") @NotNull @DurationMin(seconds = 1)
        Duration requestTimeout,

        @DefaultValue("10m") @NotNull @DurationMin(minutes = 1)
        Duration maxRunDuration,

        @DefaultValue("3") @Positive int maxAttempts) {}
