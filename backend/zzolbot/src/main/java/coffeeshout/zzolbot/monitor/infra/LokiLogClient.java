package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 능동 모니터링용 Loki 로그 클라이언트.
 * Alloy가 수집해 Loki에 적재한 ERROR/WARN 로그를 (1) 윈도우 단위로 집계하고 (2) 이상 시 샘플을 뽑는다.
 * Loki가 없거나(로컬) 조회 실패 시 0건/빈 목록으로 안전하게 떨어진다.
 */
@Slf4j
@Component
public class LokiLogClient {

    private static final String LEVEL_FILTER = "|~ \"ERROR|WARN\"";

    private final RestClient restClient;
    private final String environment;
    private final ObjectMapper objectMapper;

    public LokiLogClient(ZzolBotProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl(properties.monitoring().lokiUrl()).build();
        this.environment = properties.monitoring().environment();
        this.objectMapper = objectMapper;
    }

    /**
     * 윈도우 구간의 ERROR/WARN 로그 총 건수. count_over_time 메트릭 쿼리로 정확한 합계를 얻는다.
     */
    public long countErrors(Instant end, Duration window) {
        final String logql = String.format(
                "sum(count_over_time({environment=\"%s\"} %s [%dm]))",
                environment, LEVEL_FILTER, Math.max(1, window.toMinutes()));
        final String uri = "/loki/api/v1/query?query=" + encode(logql)
                + "&time=" + (end.toEpochMilli() * 1_000_000L);
        try {
            final String body = restClient.get().uri(uri).retrieve().body(String.class);
            return parseScalar(body);
        } catch (Exception e) {
            log.warn("[ZzolBot] Loki ERROR 로그 집계 실패 — 0건으로 처리", e);
            return 0L;
        }
    }

    /**
     * 윈도우 구간의 최근 ERROR/WARN 로그 메시지를 최대 {@code limit}건 반환(LLM 분석 근거).
     */
    public List<String> tailErrors(Instant end, Duration window, int limit) {
        final String logql = String.format("{environment=\"%s\"} %s", environment, LEVEL_FILTER);
        final long startNano = end.minus(window).toEpochMilli() * 1_000_000L;
        final long endNano = end.toEpochMilli() * 1_000_000L;
        final String uri = String.format(
                "/loki/api/v1/query_range?query=%s&start=%d&end=%d&limit=%d&direction=backward",
                encode(logql), startNano, endNano, limit);
        try {
            final String body = restClient.get().uri(uri).retrieve().body(String.class);
            return parseMessages(body, limit);
        } catch (Exception e) {
            log.warn("[ZzolBot] Loki ERROR 로그 샘플 조회 실패 — 빈 목록", e);
            return List.of();
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
            log.warn("[ZzolBot] Loki 집계 응답 파싱 실패 — 0건", e);
            return 0L;
        }
    }

    private List<String> parseMessages(String raw, int limit) {
        final List<String> messages = new ArrayList<>();
        if (raw == null) {
            return messages;
        }
        try {
            final JsonNode results = objectMapper.readTree(raw).path("data").path("result");
            if (results.isArray()) {
                for (final JsonNode stream : results) {
                    for (final JsonNode entry : stream.path("values")) {
                        if (entry.isArray() && entry.size() >= 2 && messages.size() < limit) {
                            messages.add(entry.get(1).asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[ZzolBot] Loki 로그 응답 파싱 실패 — 빈 목록", e);
        }
        return messages;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
