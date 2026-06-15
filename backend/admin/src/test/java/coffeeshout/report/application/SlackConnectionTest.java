package coffeeshout.report.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Slack webhook 실제 연동을 확인하는 수동 테스트.
 *
 * <p>실행 전 환경변수 {@code SLACK_WEBHOOK_URL}을 설정하거나 프로젝트 루트 {@code .env} 파일에 값이 있어야 합니다.
 * 연동 확인 후 {@code @Disabled}를 유지하거나 클래스를 제거합니다.
 *
 * <p>참고: {@code spring-dotenv}는 Spring 컨텍스트 안에서만 동작하므로,
 * 이 테스트는 {@code .env} 파일을 직접 파싱하여 webhook URL을 읽습니다.
 */
@Disabled("Slack 연동 확인용 수동 테스트. 실행 전 환경변수 SLACK_WEBHOOK_URL 설정 필요")
class SlackConnectionTest {

    private final RestClient restClient = RestClient.create();

    @Test
    @DisplayName("SLACK_WEBHOOK_URL 환경변수로 Slack에 테스트 메시지를 전송한다")
    void Slack_webhook에_테스트_메시지를_전송한다() {
        String webhookUrl = resolveWebhookUrl();
        assumeTrue(
                webhookUrl != null && !webhookUrl.isBlank(),
                "SLACK_WEBHOOK_URL을 찾을 수 없습니다. 환경변수 또는 .env 파일을 확인하세요."
        );

        assertThatCode(() ->
                restClient.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("text",
                                "*[테스트]* `SlackConnectionTest` — Slack 연동 확인용 메시지입니다. :white_check_mark:"))
                        .retrieve()
                        .toBodilessEntity()
        ).doesNotThrowAnyException();
    }

    /**
     * 1순위: 시스템 환경변수 SLACK_WEBHOOK_URL
     * 2순위: 프로젝트 루트 .env 파일 (spring-dotenv가 Spring 컨텍스트 외부에서는 동작하지 않으므로 직접 파싱)
     */
    private String resolveWebhookUrl() {
        String fromEnv = System.getenv("SLACK_WEBHOOK_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return readFromDotEnv("SLACK_WEBHOOK_URL");
    }

    private String readFromDotEnv(String key) {
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) {
            return null;
        }
        try {
            String prefix = key + "=";
            return Files.lines(envFile)
                    .filter(line -> line.startsWith(prefix))
                    .map(line -> line.substring(prefix.length()).trim())
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
