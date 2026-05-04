package coffeeshout.global.zzolbot.infra.tool;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
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

    public PrometheusQueryTool(ZzolBotProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(properties.monitoring().prometheusUrl()).build();
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
    public ToolExecutionResult execute(Map<String, Object> params) {
        final String query = (String) params.get("query");
        try {
            final String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/query")
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .body(String.class);
            return ToolExecutionResult.ok(TOOL_NAME, response != null ? response : "메트릭 없음");
        } catch (RestClientException e) {
            log.warn("[ZzolBot] Prometheus 조회 실패. query={}", query, e);
            return ToolExecutionResult.fail(TOOL_NAME, "Prometheus 조회 실패: " + e.getMessage());
        }
    }
}
