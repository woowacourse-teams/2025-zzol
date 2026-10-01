package coffeeshout.profanity.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(NicknameAuditProperties.class)
public class NicknameAuditConfig {

    /**
     * 검열 회차 전용 실행기.
     *
     * <p>공유 {@code virtualThreadExecutor}를 쓰지 않는 이유는 그 빈의 destroyMethod가 {@code close}라서다.
     * {@code ExecutorService.close()}는 실행 중인 작업이 끝날 때까지 블록하므로, 회차 도중 종료 신호가 오면
     * 컨텍스트 종료가 회차 예산(기본 10분)만큼 밀린다. Blue/Green 전환에서 구 컨테이너가 그만큼 늦게 내려가거나
     * SIGKILL을 맞는다. {@code shutdownNow}는 즉시 반환하고 회차 스레드에 인터럽트를 보내며, 드레인 루프가 그
     * 인터럽트를 보고 멈춘다.
     */
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService nicknameAuditExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

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
