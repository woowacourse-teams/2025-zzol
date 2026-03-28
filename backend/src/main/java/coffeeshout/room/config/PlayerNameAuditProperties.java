package coffeeshout.room.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "nickname-audit")
public record PlayerNameAuditProperties(
        String geminiApiKey,                                           // local/test는 NoOpNicknameAuditor가 사용되므로 빈 값 허용
        @NotBlank String model,
        @DecimalMin("0.0") @DecimalMax("1.0") double flaggedThreshold,
        @Positive int batchSize,                                       // Gemini API 호출 1회당 처리할 닉네임 수
        @Positive int feedbackInjectionThreshold
) {
}
