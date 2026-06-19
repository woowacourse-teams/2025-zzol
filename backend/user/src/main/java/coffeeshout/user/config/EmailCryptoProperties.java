package coffeeshout.user.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "user.email-crypto")
public record EmailCryptoProperties(
        @NotBlank
        @Size(min = 32, message = "이메일 암호화 키는 최소 32자 이상이어야 합니다.")
        String encryptionKey,
        @NotBlank
        @Size(min = 32, message = "이메일 블라인드 인덱스 HMAC 키는 최소 32자 이상이어야 합니다.")
        String hmacKey
) {
}
