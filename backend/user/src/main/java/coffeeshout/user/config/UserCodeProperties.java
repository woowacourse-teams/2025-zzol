package coffeeshout.user.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "user.code")
public record UserCodeProperties(
        @Positive int maxRetry
) {
}
