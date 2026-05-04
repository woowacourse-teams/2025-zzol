package coffeeshout.global.zzolbot.infra.tool;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@WireMockTest
class PrometheusQueryToolTest {

    private PrometheusQueryTool createTool(WireMockRuntimeInfo wmInfo) {
        final ZzolBotProperties props = new ZzolBotProperties(
                "",
                "gemini-2.0-flash",
                8,
                new ZzolBotProperties.MonitoringProperties(
                        "http://loki",
                        "http://tempo",
                        wmInfo.getHttpBaseUrl(),
                        "local"
                )
        );
        return new PrometheusQueryTool(props, RestClient.builder());
    }

    @Nested
    class execute_메서드 {

        @Test
        void Prometheus_응답_성공_시_ok_결과를_반환한다(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlPathEqualTo("/api/v1/query"))
                    .willReturn(ok().withBody("{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[]}}")));

            final ToolExecutionResult result = createTool(wmInfo)
                    .execute(Map.of("query", "redis_stream_lag_seconds"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isTrue();
                softly.assertThat(result.toolName()).isEqualTo(PrometheusQueryTool.TOOL_NAME);
                softly.assertThat(result.content()).contains("success");
            });
        }

        @Test
        void Prometheus_서버_오류_시_실패_결과를_반환한다(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlPathEqualTo("/api/v1/query"))
                    .willReturn(serverError()));

            final ToolExecutionResult result = createTool(wmInfo)
                    .execute(Map.of("query", "redis_stream_lag_seconds"));

            assertThat(result.success()).isFalse();
        }
    }
}
