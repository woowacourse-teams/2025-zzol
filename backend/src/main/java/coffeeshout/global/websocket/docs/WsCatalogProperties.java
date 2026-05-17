package coffeeshout.global.websocket.docs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "websocket.docs")
public record WsCatalogProperties(
        @NotBlank String appPath,
        @NotBlank String topicPath,
        @NotBlank String queuePath,
        @NotBlank String userDestinationPrefix,
        @NotBlank String stompEndpoint,
        @NotBlank String errorTopic,
        @NotNull Class<?> envelopeClass,
        @NotEmpty List<String> allowedIps
) {
}
