package coffeeshout.profanity.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
        @DefaultValue("2") @Positive int minTermLength
) {
}
