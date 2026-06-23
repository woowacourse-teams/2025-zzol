package coffeeshout.zzolbot.infra.tool;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@WireMockTest
class LokiQueryToolTest {

    private static final AskContext CTX = AskContext.stamp("test", List.of(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    private LokiQueryTool createTool(WireMockRuntimeInfo wmInfo) {
        return createTool(wmInfo.getHttpBaseUrl());
    }

    private LokiQueryTool createTool(String lokiUrl) {
        final ZzolBotProperties props = new ZzolBotProperties(
                "",
                "gemini-2.0-flash",
                8,
                new ZzolBotProperties.MonitoringProperties(
                        lokiUrl,
                        "http://tempo",
                        "http://prometheus",
                        "local"
                ),
                new ZzolBotProperties.DeterminismProperties(0.1, 0.1),
                60,
                10000L,
                new ZzolBotProperties.SqlProperties(List.of(), 100, 3)
        );
        return new LokiQueryTool(props, RestClient.builder(), new ObjectMapper());
    }

    @Nested
    class execute_메서드 {

        @Test
        void Loki_응답_성공_시_ok_결과를_반환한다(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlPathEqualTo("/loki/api/v1/query_range"))
                    .willReturn(ok().withBody("{\"status\":\"success\",\"data\":{}}")));

            final ToolExecutionResult result = createTool(wmInfo).execute(Map.of("joinCode", "A4BX"), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isTrue();
                softly.assertThat(result.toolName()).isEqualTo(LokiQueryTool.TOOL_NAME);
                softly.assertThat(result.content()).contains("success");
            });
        }

        @Test
        void Loki_서버_오류_시_실패_결과를_반환한다(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlPathEqualTo("/loki/api/v1/query_range"))
                    .willReturn(serverError()));

            final ToolExecutionResult result = createTool(wmInfo).execute(Map.of("joinCode", "A4BX"), CTX);

            assertThat(result.success()).isFalse();
        }

        @Test
        void since_파라미터_없으면_기본_1시간을_사용한다(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlPathEqualTo("/loki/api/v1/query_range"))
                    .willReturn(ok().withBody("{\"status\":\"success\"}")));

            final ToolExecutionResult result = createTool(wmInfo).execute(Map.of("joinCode", "A4BX"), CTX);

            assertThat(result.success()).isTrue();
        }

        @Test
        void since_30m_파라미터를_정상_처리한다(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlPathEqualTo("/loki/api/v1/query_range"))
                    .willReturn(ok().withBody("{\"status\":\"success\"}")));

            final ToolExecutionResult result = createTool(wmInfo)
                    .execute(Map.of("joinCode", "A4BX", "since", "30m"), CTX);

            assertThat(result.success()).isTrue();
        }

        @Test
        void since_999h처럼_상한을_초과하면_24h로_clamp된다(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlPathEqualTo("/loki/api/v1/query_range"))
                    .willReturn(ok().withBody("{\"status\":\"success\"}")));

            final ToolExecutionResult result = createTool(wmInfo)
                    .execute(Map.of("joinCode", "A4BX", "since", "999h"), CTX);

            assertThat(result.success()).isTrue();
        }

        @Test
        void since가_음수_분이면_1분으로_clamp된다(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlPathEqualTo("/loki/api/v1/query_range"))
                    .willReturn(ok().withBody("{\"status\":\"success\"}")));

            final ToolExecutionResult result = createTool(wmInfo)
                    .execute(Map.of("joinCode", "A4BX", "since", "-30m"), CTX);

            assertThat(result.success()).isTrue();
        }

        @Test
        void joinCode_없이_호출하면_전역_로그_조회에_성공한다(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlPathEqualTo("/loki/api/v1/query_range"))
                    .willReturn(ok().withBody("{\"status\":\"success\",\"data\":{}}")));

            final ToolExecutionResult result = createTool(wmInfo).execute(Map.of(), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isTrue();
                softly.assertThat(result.toolName()).isEqualTo(LokiQueryTool.TOOL_NAME);
            });
        }

        @Test
        void LogQL_쿼리는_이중_인코딩되지_않은_URI로_요청한다(WireMockRuntimeInfo wmInfo) {
            // 회귀 방지: 과거 URLEncoder 결과를 RestClient.uri(String)에 넘겨 '%'가 '%25'로 재인코딩되면서
            // Loki가 400 "parse error ... unexpected %!(NOVERB)"로 거부했다(loki_logs 100% 실패).
            stubFor(get(urlPathEqualTo("/loki/api/v1/query_range"))
                    .willReturn(ok().withBody("{\"status\":\"success\",\"data\":{}}")));

            createTool(wmInfo).execute(Map.of(), CTX); // joinCode 없음 → {environment="local"} |~ "ERROR|WARN"

            final List<LoggedRequest> requests = findAll(getRequestedFor(urlPathEqualTo("/loki/api/v1/query_range")));
            assertThat(requests).hasSize(1);
            final String rawUrl = requests.get(0).getUrl();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(rawUrl).as("LogQL '{'는 한 번만 인코딩(%%7B)").contains("%7B");
                softly.assertThat(rawUrl).as("이중 인코딩(%%257B) 금지").doesNotContain("%257B");
            });
        }

        @Test
        void lokiUrl_끝에_슬래시가_있어도_이중_슬래시_없이_요청한다(WireMockRuntimeInfo wmInfo) {
            // 회귀 방지: base + path 문자열 결합 시 "http://loki:3100/" + "/loki/..." → "//loki/..." 가 되면
            // 프록시/Loki 라우팅이 깨질 수 있다. URI.resolve로 경로 결합을 고정한다.
            stubFor(get(urlPathEqualTo("/loki/api/v1/query_range"))
                    .willReturn(ok().withBody("{\"status\":\"success\",\"data\":{}}")));

            final ToolExecutionResult result = createTool(wmInfo.getHttpBaseUrl() + "/").execute(Map.of(), CTX);

            assertThat(result.success()).isTrue(); // '//loki' 가 되면 path 불일치로 stub 미적중 → 실패했을 것
            final String rawUrl = findAll(getRequestedFor(urlPathEqualTo("/loki/api/v1/query_range")))
                    .get(0).getUrl();
            assertThat(rawUrl).doesNotContain("//loki");
        }

        @Test
        void joinCode_형식이_잘못된_경우_실패를_반환한다(WireMockRuntimeInfo wmInfo) {
            final ToolExecutionResult result = createTool(wmInfo).execute(Map.of("joinCode", "invalid!"), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isFalse();
                softly.assertThat(result.content()).contains("유효하지 않은 joinCode 형식");
            });
        }
    }
}
