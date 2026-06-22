package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 능동 모니터링용 Prometheus 메트릭 클라이언트.
 * HTTP 5xx 응답 수(윈도우 집계)와 Redis Stream 컨슈머 스레드풀의 큐 깊이(순간값)를 조회한다.
 * Prometheus가 없거나(로컬) 조회 실패 시 0으로 안전하게 떨어진다.
 */
@Slf4j
@Component
public class PrometheusMetricClient {

    private final RestClient restClient;
    private final String prometheusBaseUrl;
    private final ObjectMapper objectMapper;

    public PrometheusMetricClient(
            ZzolBotProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.prometheusBaseUrl = properties.monitoring().prometheusUrl();
        this.restClient = restClientBuilder.baseUrl(prometheusBaseUrl).build();
        this.objectMapper = objectMapper;
    }

    /**
     * 윈도우 구간의 HTTP 5xx 응답 총 건수.
     */
    public long count5xx(Instant end, Duration window) {
        final String promql = String.format(
                "sum(increase(http_server_requests_seconds_count{status=~\"5..\"}[%dm]))",
                Math.max(1, window.toMinutes()));
        return queryScalar(promql, end, "5xx 집계");
    }

    /**
     * 모든 Redis Stream 컨슈머 스레드풀 큐 깊이의 최댓값(= 처리 backpressure).
     * XLEN은 MAXLEN trimming 때문에 컨슈머가 죽어도 일정하게 유지돼 lag 지표가 되지 못한다.
     * 진짜로 처리가 밀리는지는 컨슈머 스레드풀의 대기 큐에서 드러나므로 그 게이지를 본다.
     */
    public long maxConsumerQueueSize(Instant at) {
        return queryScalar("max(redis_stream_threadpool_queue_size)", at, "컨슈머 큐 깊이");
    }

    private long queryScalar(String promql, Instant at, String label) {
        // 쿼리를 직접 인코딩한 뒤 String이 아닌 URI로 넘긴다.
        // RestClient.uri(String)은 인자를 URI 템플릿으로 보고 한 번 더 인코딩해(% → %25) PromQL을 깨뜨린다.
        final URI uri = URI.create(prometheusBaseUrl).resolve(
                "/api/v1/query?query=" + encode(promql) + "&time=" + at.getEpochSecond());
        try {
            final String body = restClient.get().uri(uri).retrieve().body(String.class);
            return parseScalar(body);
        } catch (Exception e) {
            log.warn("[ZzolBot] Prometheus {} 실패 — 0으로 처리", label, e);
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
            log.warn("[ZzolBot] Prometheus 응답 파싱 실패 — 0으로 처리", e);
            return 0L;
        }
    }

    private String encode(String value) {
        // URLEncoder는 공백을 '+'로 만든다. URI로 직접 넘기므로 어디서나 일관되게 해석되는 %20으로 통일한다.
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
