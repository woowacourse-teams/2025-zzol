package coffeeshout.user.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "user.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Positive long accessTokenExpirationSeconds,
        @Positive long refreshTokenExpirationSeconds
) {
}
