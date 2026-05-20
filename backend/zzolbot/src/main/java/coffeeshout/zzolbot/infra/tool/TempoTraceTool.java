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
import org.springframework.web.util.UriBuilder;

@Slf4j
@Component
public class TempoTraceTool implements ZzolBotTool {

    static final String TOOL_NAME = "tempo_traces";
    private static final int TRACE_LIMIT = 20;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TempoTraceTool(ZzolBotProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl(properties.monitoring().tempoUrl()).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "Tempo에서 분산 트레이스를 조회한다. " +
                "joinCode가 있으면 해당 방의 요청 흐름을 조회하고, 없으면 최근 전체 트레이스를 조회한다.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "joinCode", Map.of(
                                "type", "string",
                                "description", "4자리 방 입장 코드. 생략하면 전체 트레이스 조회"
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> params, AskContext ctx) {
        final Object rawJoinCode = params.get("joinCode");
        if (rawJoinCode instanceof String s && !s.isBlank() && !s.matches("[A-Z0-9]{4}")) {
            return ToolExecutionResult.fail(TOOL_NAME, "유효하지 않은 joinCode 형식");
        }
        final String joinCode = (rawJoinCode instanceof String s && s.matches("[A-Z0-9]{4}")) ? s : null;
        try {
            final String response = restClient.get()
                    .uri(uriBuilder -> {
                        final UriBuilder builder = uriBuilder.path("/api/search").queryParam("limit", TRACE_LIMIT);
                        if (joinCode != null) {
                            builder.queryParam("tags", "joinCode=" + joinCode);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(String.class);
            return ToolExecutionResult.ok(TOOL_NAME, parseTempoResponse(response));
        } catch (RestClientException e) {
            log.warn("[ZzolBot] Tempo 조회 실패. joinCode={}", joinCode, e);
            return ToolExecutionResult.fail(TOOL_NAME, "Tempo 조회 실패");
        }
    }

    private String parseTempoResponse(String raw) {
        if (raw == null) {
            return "트레이스 없음";
        }
        try {
            final JsonNode root = objectMapper.readTree(raw);
            final ArrayNode traces = objectMapper.createArrayNode();

            final JsonNode rawTraces = root.path("traces");
            if (rawTraces.isArray()) {
                for (final JsonNode t : rawTraces) {
                    final ObjectNode trace = objectMapper.createObjectNode();
                    trace.put("traceId", t.path("traceID").asText());
                    trace.put("service", t.path("rootServiceName").asText());
                    trace.put("operation", t.path("rootTraceName").asText());
                    trace.put("durationMs", t.path("durationMs").asLong());
                    traces.add(trace);
                }
            }

            final ObjectNode summary = objectMapper.createObjectNode();
            summary.put("traceCount", traces.size());
            summary.set("traces", traces);
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            log.warn("[ZzolBot] Tempo 응답 파싱 실패, raw 응답 반환");
            return raw;
        }
    }
}
