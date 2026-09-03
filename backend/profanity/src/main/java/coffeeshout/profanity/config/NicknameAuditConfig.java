package coffeeshout.profanity.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
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
                .httpOptions(httpOptions(properties))
                .build();
    }

    /**
     * SDK가 빌드된 {@link Client}에서 타임아웃을 되읽을 방법을 주지 않아, 검증 가능하도록 조립부를 분리한다.
     * {@code HttpOptions.timeout}은 밀리초 단위다.
     */
    static HttpOptions httpOptions(NicknameAuditProperties properties) {
        return HttpOptions.builder()
                .timeout(Math.toIntExact(properties.requestTimeout().toMillis()))
                .build();
    }
}
