package coffeeshout.user.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "user.jwt")
public record JwtProperties(
        @NotBlank
        @Size(min = 32, message = "JWT secret은 HS256 최소 키 길이(32자) 이상이어야 합니다. ASCII 문자만 사용하세요.")
        @Pattern(regexp = "^[\\x20-\\x7E]+$", message = "JWT secret은 ASCII 문자만 허용됩니다. 멀티바이트 문자 사용 시 실제 바이트 길이가 32바이트 미만이 될 수 있습니다.")
        String secret,
        @Positive long accessTokenExpirationSeconds,
        @Positive long refreshTokenExpirationSeconds
) {
}
