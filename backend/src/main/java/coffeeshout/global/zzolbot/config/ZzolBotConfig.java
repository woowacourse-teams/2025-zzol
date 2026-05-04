package coffeeshout.global.zzolbot.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties(ZzolBotProperties.class)
public class ZzolBotConfig {

    @Bean("zzolBotClient")
    @Profile("!local & !test")
    public Client zzolBotClient(ZzolBotProperties properties) {
        return Client.builder()
                .apiKey(properties.geminiApiKey())
                .build();
    }
}
