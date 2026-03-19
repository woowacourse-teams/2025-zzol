package coffeeshout.bombrelay.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bomb-relay.dictionary")
public record BombRelayDictionaryProperties(
        @NotNull String apiUrl,
        @NotNull String apiKey
) {
}
