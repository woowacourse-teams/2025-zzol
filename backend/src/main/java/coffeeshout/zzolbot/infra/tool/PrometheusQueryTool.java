package coffeeshout.zzolbot.infra.tool;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class PrometheusQueryTool implements ZzolBotTool {

    static final String TOOL_NAME = "prometheus_query";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PrometheusQueryTool(ZzolBotProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl(properties.monitoring().prometheusUrl()).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "Prometheus에 PromQL 쿼리를 실행한다. " +
                "Redis Stream lag, 활성 방 수, HTTP 에러율 등의 메트릭을 조회할 때 사용한다. " +
                "예: 'redis_stream_lag_seconds{stream=\"room\"}'";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "description", "PromQL 표현식"
                        )
                ),
                "required", List.of("query")
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> params, AskContext ctx) {
        final String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return ToolExecutionResult.fail(TOOL_NAME, "query 파라미터가 누락되었습니다.");
        }
        try {
            final String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/query")
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .body(String.class);
            return ToolExecutionResult.ok(TOOL_NAME, parsePrometheusResponse(response));
        } catch (RestClientException e) {
            log.warn("[ZzolBot] Prometheus 조회 실패. query={}", query, e);
            return ToolExecutionResult.fail(TOOL_NAME, "Prometheus 조회 실패");
        }
    }

    private String parsePrometheusResponse(String raw) {
        if (raw == null) {
            return "메트릭 없음";
        }
        try {
            final JsonNode root = objectMapper.readTree(raw);
            final String status = root.path("status").asText("unknown");
            final ArrayNode metrics = objectMapper.createArrayNode();

            final JsonNode results = root.path("data").path("result");
            if (results.isArray()) {
                for (final JsonNode item : results) {
                    final ObjectNode metric = objectMapper.createObjectNode();
                    metric.set("labels", item.path("metric"));
                    final JsonNode value = item.path("value");
                    if (value.isArray() && value.size() >= 2) {
                        metric.put("value", value.get(1).asText());
                    }
                    metrics.add(metric);
                }
            }

            final ObjectNode summary = objectMapper.createObjectNode();
            summary.put("status", status);
            summary.set("metrics", metrics);
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            log.warn("[ZzolBot] Prometheus 응답 파싱 실패, raw 응답 반환");
            return raw;
        }
    }
}
