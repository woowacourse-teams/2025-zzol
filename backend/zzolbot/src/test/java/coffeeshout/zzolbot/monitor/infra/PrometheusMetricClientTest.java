package coffeeshout.zzolbot.monitor.infra;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@WireMockTest
class PrometheusMetricClientTest {

    private PrometheusMetricClient createClient(WireMockRuntimeInfo wmInfo) {
        final ZzolBotProperties props = new ZzolBotProperties(
                "",
                "gemini-2.0-flash",
                8,
                new ZzolBotProperties.MonitoringProperties(
                        "http://loki",
                        "http://tempo",
                        wmInfo.getHttpBaseUrl(),
                        "local"
                ),
                new ZzolBotProperties.DeterminismProperties(0.1, 0.1),
                60,
                10000L,
                new ZzolBotProperties.SqlProperties(List.of(), 100, 3)
        );
        return new PrometheusMetricClient(props, RestClient.builder(), new ObjectMapper());
    }

    @Test
    void 컨슈머_스레드풀_큐_깊이_게이지를_파싱한다(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlPathEqualTo("/api/v1/query"))
                .willReturn(ok().withBody("{\"status\":\"success\",\"data\":{\"result\":[{\"value\":[1718000000,\"600\"]}]}}")));

        final long depth = createClient(wmInfo).maxConsumerQueueSize(Instant.EPOCH);

        assertThat(depth).isEqualTo(600);
    }

    @Test
    void PromQL은_이중_인코딩되지_않은_URI로_요청한다(WireMockRuntimeInfo wmInfo) {
        // 회귀 방지: URLEncoder 결과를 RestClient.uri(String)에 넘기면 '%'가 '%25'로 재인코딩돼 PromQL이 깨진다.
        stubFor(get(urlPathEqualTo("/api/v1/query"))
                .willReturn(ok().withBody("{\"status\":\"success\",\"data\":{\"result\":[]}}")));

        createClient(wmInfo).count5xx(Instant.EPOCH, Duration.ofMinutes(60)); // PromQL에 '{','}' 포함

        final String rawUrl = findAll(getRequestedFor(urlPathEqualTo("/api/v1/query"))).get(0).getUrl();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(rawUrl).as("PromQL '{'는 한 번만 인코딩(%%7B)").contains("%7B");
            softly.assertThat(rawUrl).as("이중 인코딩(%%257B) 금지").doesNotContain("%257B");
        });
    }

    @Test
    void Prometheus_오류_시_0으로_떨어진다(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlPathEqualTo("/api/v1/query")).willReturn(serverError()));

        assertThat(createClient(wmInfo).maxConsumerQueueSize(Instant.EPOCH)).isZero();
    }
}
