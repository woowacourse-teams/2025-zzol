package coffeeshout.profanity.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "nickname-audit")
public record NicknameAuditProperties(
        String geminiApiKey,
        @NotBlank String model,
        @DecimalMin("0.0") @DecimalMax("1.0") double flaggedThreshold,
        @Positive int batchSize,
        @Positive int feedbackInjectionThreshold,
        @DefaultValue("2") @Positive int minTermLength,
        /**
         * Gemini HTTP 요청 타임아웃. 없으면 응답이 끝내 오지 않는 호출 하나가 스케줄러 스레드를
         * 무기한 점유한다. {@code batchSize}를 키우면 응답 생성 시간도 늘어나므로 함께 올려야 한다.
         */
        @DefaultValue("120s") @NotNull Duration requestTimeout,
        /**
         * 한 회차가 스케줄러에서 넘겨받은 스레드를 붙잡아도 되는 상한. 넘기면 남은 적체는 다음 회차로 넘긴다.
         * {@code ProfanityAuditService.LOCK_LEASE_MILLIS}보다 반드시 짧아야 한다.
         */
        @DefaultValue("10m") @NotNull Duration maxRunDuration) {}
