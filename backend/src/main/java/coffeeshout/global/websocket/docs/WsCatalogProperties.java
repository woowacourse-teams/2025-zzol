package coffeeshout.global.websocket.docs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
        @NotBlank String envelopeType,
        // tools/ws-mcp/ MCP 서버가 STOMP 연결 시 사용한다. 카탈로그 JSON 응답에는 포함되지 않는다.
        @NotBlank String serverUrl,
        @Valid @NotNull Info info
) {

    public record Info(
            @NotBlank String title,
            @NotBlank String version,
            @NotBlank String description
    ) {
    }
}
