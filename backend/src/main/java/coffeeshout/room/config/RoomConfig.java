package coffeeshout.room.config;

import com.google.genai.Client;
import com.vane.badwordfiltering.BadWordFiltering;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class RoomConfig {

    @Bean
    public BadWordFiltering badWordFiltering() {
        return new BadWordFiltering();
    }

    @Bean
    @Profile("!local & !test")
    public Client geminiClient(NicknameAuditProperties properties) {
        return Client.builder()
                .apiKey(properties.geminiApiKey())
                .build();
    }
}
