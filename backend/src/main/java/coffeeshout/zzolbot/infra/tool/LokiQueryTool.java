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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class LokiQueryTool implements ZzolBotTool {

    static final String TOOL_NAME = "loki_logs";
    private static final int DEFAULT_LOOK_BACK_MINUTES = 60;
    private static final int MIN_LOOK_BACK_MINUTES = 1;
    private static final int MAX_LOOK_BACK_MINUTES = 1440;
    private static final int LOG_LIMIT = 50;

    private final RestClient restClient;
    private final String environment;
    private final ObjectMapper objectMapper;

    public LokiQueryTool(ZzolBotProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl(properties.monitoring().lokiUrl()).build();
        this.environment = properties.monitoring().environment();
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    private static final String GLOBAL_LOG_FILTER = "|~ \"ERROR|WARN\"";

    @Override
    public String description() {
        return "Loki에서 로그를 조회한다. " +
                "joinCode가 있으면 해당 방으로 필터링하고, 없으면 전체 환경에서 ERROR/WARN 레벨을 조회한다. " +
                "since 파라미터로 조회 기간을 지정한다 (예: 30m, 1h, 2h). 기본값은 1h.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "joinCode", Map.of(
                                "type", "string",
                                "description", "4자리 방 입장 코드. 생략하면 전체 환경 조회"
                        ),
                        "since", Map.of(
                                "type", "string",
                                "description", "조회 기간 (예: 30m, 1h, 2h). 기본값 1h"
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> params, AskContext ctx) {
        final Object rawJoinCode = params.get("joinCode");
        if (rawJoinCode instanceof String s && !s.isBlank() && !isValidJoinCode(s)) {
            return ToolExecutionResult.fail(TOOL_NAME, "유효하지 않은 joinCode 형식");
        }
        final String joinCode = (rawJoinCode instanceof String s && isValidJoinCode(s)) ? s : null;
        final int lookBackMinutes = parseLookBackMinutes((String) params.getOrDefault("since", "1h"));

        final Instant end = ctx.asOf();
        final Instant start = end.minus(lookBackMinutes, ChronoUnit.MINUTES);

        final String lokiQuery = joinCode != null
                ? String.format("{environment=\"%s\"} |= \"%s\"", environment, joinCode)
                : String.format("{environment=\"%s\"} %s", environment, GLOBAL_LOG_FILTER);
        final long startNano = start.toEpochMilli() * 1_000_000L;
        final long endNano = end.toEpochMilli() * 1_000_000L;
        final String encodedUri = String.format(
                "/loki/api/v1/query_range?query=%s&start=%d&end=%d&limit=%d",
                URLEncoder.encode(lokiQuery, StandardCharsets.UTF_8),
                startNano, endNano, LOG_LIMIT
        );

        try {
            final String response = restClient.get()
                    .uri(encodedUri)
                    .retrieve()
                    .body(String.class);
            return ToolExecutionResult.ok(TOOL_NAME, parseLokiResponse(response));
        } catch (RestClientException e) {
            log.warn("[ZzolBot] Loki 조회 실패. joinCode={}", joinCode, e);
            return ToolExecutionResult.fail(TOOL_NAME, "Loki 조회 실패");
        }
    }

    private String parseLokiResponse(String raw) {
        if (raw == null) {
            return "로그 없음";
        }
        try {
            final JsonNode root = objectMapper.readTree(raw);
            final String status = root.path("status").asText("unknown");
            final ArrayNode logs = objectMapper.createArrayNode();

            final JsonNode results = root.path("data").path("result");
            if (results.isArray()) {
                for (final JsonNode stream : results) {
                    final JsonNode values = stream.path("values");
                    if (values.isArray()) {
                        for (final JsonNode entry : values) {
                            if (entry.isArray() && entry.size() >= 2) {
                                final ObjectNode log = objectMapper.createObjectNode();
                                log.put("time", Instant.ofEpochMilli(entry.get(0).asLong() / 1_000_000).toString());
                                log.put("message", entry.get(1).asText());
                                logs.add(log);
                            }
                        }
                    }
                }
            }

            final ObjectNode summary = objectMapper.createObjectNode();
            summary.put("status", status);
            summary.put("logCount", logs.size());
            summary.set("logs", logs);
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            log.warn("[ZzolBot] Loki 응답 파싱 실패, raw 응답 반환");
            return raw;
        }
    }

    private boolean isValidJoinCode(String joinCode) {
        return joinCode != null && joinCode.matches("[A-Z0-9]{4}");
    }

    private int parseLookBackMinutes(String since) {
        if (since == null || since.isBlank()) {
            return DEFAULT_LOOK_BACK_MINUTES;
        }
        try {
            final int minutes;
            if (since.endsWith("h")) {
                minutes = Integer.parseInt(since.replace("h", "")) * 60;
            } else if (since.endsWith("m")) {
                minutes = Integer.parseInt(since.replace("m", ""));
            } else {
                return DEFAULT_LOOK_BACK_MINUTES;
            }
            return Math.max(MIN_LOOK_BACK_MINUTES, Math.min(MAX_LOOK_BACK_MINUTES, minutes));
        } catch (NumberFormatException ignored) {
        }
        return DEFAULT_LOOK_BACK_MINUTES;
    }
}
