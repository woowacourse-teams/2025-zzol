package coffeeshout.profanity.config;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
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
        final NicknameAuditProperties properties = NicknameAuditPropertiesFixture.요청_타임아웃(Duration.ofSeconds(90));

        final HttpOptions httpOptions = NicknameAuditConfig.httpOptions(properties);

        assertThat(httpOptions.timeout())
                .as("HttpOptions.timeout 은 밀리초 단위다. 초 단위 값을 그대로 넣으면 90ms 가 되어 모든 호출이 즉시 끊긴다.")
                .contains(90_000);
    }

    /**
     * SDK는 재시도 설정이 없으면 408·429·5xx 응답에 같은 요청을 다섯 번까지 다시 보낸다
     * ({@code com.google.genai.RetryInterceptor}). 그 재시도는 SDK 안에서 일어나 하루 호출 상한이 세지 못한다.
     */
    @Test
    void SDK_내부_재시도를_꺼서_호출_한_번이_HTTP_요청_한_번이_되게_한다() {
        final NicknameAuditProperties properties = NicknameAuditPropertiesFixture.요청_타임아웃(Duration.ofSeconds(90));

        final HttpOptions httpOptions = NicknameAuditConfig.httpOptions(properties);

        assertThat(httpOptions.retryOptions().flatMap(HttpRetryOptions::attempts))
                .as("재시도가 켜져 있으면 상한 400회가 실제로는 요청 2000회까지 나갈 수 있다(#1907).")
                .contains(1);
    }
}
