package coffeeshout.user.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "user.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "JWT secret은 HS256 최소 키 길이(32바이트) 이상이어야 합니다.") String secret,
        @Positive long accessTokenExpirationSeconds,
        @Positive long refreshTokenExpirationSeconds
) {
}
