package coffeeshout.room.config;

import coffeeshout.common.nickname.WordPicker;
import com.google.genai.Client;
import com.vane.badwordfiltering.BadWordFiltering;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class RoomConfig {

    @Bean
    public BadWordFiltering badWordFiltering() {
        return new BadWordFiltering();
    }

    @Bean("nicknameAuditClient")
    @Profile("!local & !test")
    public Client geminiClient(PlayerNameAuditProperties properties) {
        return Client.builder()
                .apiKey(properties.geminiApiKey())
                .build();
    }

    @Bean
    public WordPicker wordPicker() {
        return words -> words.get(ThreadLocalRandom.current().nextInt(words.size()));
    }
}
