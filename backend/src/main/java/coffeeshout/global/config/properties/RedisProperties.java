package coffeeshout.global.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.data.redis")
public record RedisProperties(
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        Ssl ssl
) {
    public record Ssl(boolean enabled) {
    }
}
