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
        @DefaultValue("120s") @NotNull Duration requestTimeout) {}
