package coffeeshout.profanity.config;

import com.google.genai.Client;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(NicknameAuditProperties.class)
public class NicknameAuditConfig {

    @Bean("nicknameAuditClient")
    @Profile("!local & !test")
    public Client geminiClient(NicknameAuditProperties properties) {
        if (!StringUtils.hasText(properties.geminiApiKey())) {
            throw new IllegalStateException("nickname-audit.gemini-api-key must not be blank");
        }
        return Client.builder()
                .apiKey(properties.geminiApiKey())
                .build();
    }
}
