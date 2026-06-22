package coffeeshout.zzolbot.config;

import coffeeshout.zzolbot.domain.PiiMasker;
import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import com.google.genai.Client;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties({ZzolBotProperties.class, MonitorProperties.class})
public class ZzolBotConfig {

    @Bean
    public PiiMasker piiMasker() {
        return new PiiMasker();
    }

    @Bean("zzolBotClient")
    @Profile("!test")
    public Client zzolBotClient(ZzolBotProperties properties) {
        final String apiKey = properties.geminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_ZZOL_BOT_API_KEY가 설정되지 않았습니다.");
        }
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }
}
