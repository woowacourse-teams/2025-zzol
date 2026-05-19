package coffeeshout.room.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class GeminiAuditConfig {

    @Bean("nicknameAuditClient")
    @Profile("!local & !test")
    public Client geminiClient(PlayerNameAuditProperties properties) {
        return Client.builder()
                .apiKey(properties.geminiApiKey())
                .build();
    }
}
