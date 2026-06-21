package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 능동 모니터링용 Prometheus 메트릭 클라이언트.
 * HTTP 5xx 응답 수를 윈도우 단위로 집계한다. outbox/redis/로그가 못 잡는 서버 오류를
 * 로깅 여부와 무관하게 응답 메트릭 자체로 포착한다.
 * Prometheus가 없거나(로컬) 조회 실패 시 0건으로 안전하게 떨어진다.
 */
@Slf4j
@Component
public class PrometheusMetricClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PrometheusMetricClient(
            ZzolBotProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl(properties.monitoring().prometheusUrl()).build();
        this.objectMapper = objectMapper;
    }

    /**
     * 윈도우 구간의 HTTP 5xx 응답 총 건수.
     */
    public long count5xx(Instant end, Duration window) {
        final String promql = String.format(
                "sum(increase(http_server_requests_seconds_count{status=~\"5..\"}[%dm]))",
                Math.max(1, window.toMinutes()));
        final String uri = "/api/v1/query?query=" + encode(promql)
                + "&time=" + end.getEpochSecond();
        try {
            final String body = restClient.get().uri(uri).retrieve().body(String.class);
            return parseScalar(body);
        } catch (Exception e) {
            log.warn("[ZzolBot] Prometheus 5xx 집계 실패 — 0건으로 처리", e);
            return 0L;
        }
    }

    private long parseScalar(String raw) {
        if (raw == null) {
            return 0L;
        }
        try {
            final JsonNode result = objectMapper.readTree(raw).path("data").path("result");
            if (result.isArray() && !result.isEmpty()) {
                final JsonNode value = result.get(0).path("value");
                if (value.isArray() && value.size() >= 2) {
                    return (long) Double.parseDouble(value.get(1).asText("0"));
                }
            }
            return 0L;
        } catch (Exception e) {
            log.warn("[ZzolBot] Prometheus 응답 파싱 실패 — 0건", e);
            return 0L;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
