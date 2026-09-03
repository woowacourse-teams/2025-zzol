package coffeeshout.profanity.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.types.HttpOptions;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Gemini 클라이언트에 HTTP 타임아웃이 걸리는지 검증한다.
 *
 * <p>타임아웃이 없으면 응답이 끝내 오지 않는 호출 하나가 스케줄러 스레드를 무기한 점유한다.
 * 레이트리미터의 {@code timeout-duration}은 허용량을 기다리는 시간이지 HTTP 타임아웃이 아니라
 * 이걸 대신하지 못한다.
 */
class NicknameAuditConfigTest {

    @Test
    void 요청_타임아웃이_밀리초로_변환되어_설정된다() {
        final NicknameAuditProperties properties = propertiesWithTimeout(Duration.ofSeconds(90));

        final HttpOptions httpOptions = NicknameAuditConfig.httpOptions(properties);

        assertThat(httpOptions.timeout())
                .as("HttpOptions.timeout 은 밀리초 단위다. 초 단위 값을 그대로 넣으면 90ms 가 되어 모든 호출이 즉시 끊긴다.")
                .contains(90_000);
    }

    private NicknameAuditProperties propertiesWithTimeout(Duration timeout) {
        return new NicknameAuditProperties("api-key", "gemini-3.5-flash", 0.85, 100, 20, 2, timeout);
    }
}
