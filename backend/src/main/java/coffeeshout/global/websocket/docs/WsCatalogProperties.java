package coffeeshout.global.websocket.docs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "websocket.docs")
public record WsCatalogProperties(
        boolean enabled,
        String basePackage,
        String appPath,
        String topicPath,
        String serverUrl,
        Info info
) {

    public record Info(
            String title,
            String version,
            String description
    ) {
    }
}
