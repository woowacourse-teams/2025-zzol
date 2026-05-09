package coffeeshout.global.zzolbot.infra.tool;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.AskContext;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
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
        final ZzolBotProperties props = new ZzolBotProperties(
                "",
                "gemini-2.0-flash",
                8,
                new ZzolBotProperties.MonitoringProperties(
                        wmInfo.getHttpBaseUrl(),
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
    }
}
