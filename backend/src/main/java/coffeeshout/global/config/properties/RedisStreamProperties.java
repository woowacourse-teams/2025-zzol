package coffeeshout.global.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.data.redis.stream")
public record RedisStreamProperties(
        @NotBlank String roomJoinKey,
        @NotBlank String cardGameSelectKey,
        @Positive int maxLength
) {
}
