package coffeeshout.global.zzolbot.infra.tool;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
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
    private static final int LOG_LIMIT = 50;

    private final RestClient restClient;

    public LokiQueryTool(ZzolBotProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(properties.monitoring().lokiUrl()).build();
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "Loki에서 joinCode가 포함된 최근 로그를 조회한다. " +
                "since 파라미터로 조회 기간을 지정한다 (예: 30m, 1h, 2h). 기본값은 1h.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "joinCode", Map.of(
                                "type", "string",
                                "description", "4자리 방 입장 코드"
                        ),
                        "since", Map.of(
                                "type", "string",
                                "description", "조회 기간 (예: 30m, 1h, 2h). 기본값 1h"
                        )
                ),
                "required", List.of("joinCode")
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> params) {
        final String joinCodeValue = (String) params.get("joinCode");
        final int lookBackMinutes = parseLookBackMinutes((String) params.getOrDefault("since", "1h"));

        final Instant end = Instant.now();
        final Instant start = end.minus(lookBackMinutes, ChronoUnit.MINUTES);

        final String logqlQuery = String.format("{app=\"zzol-backend\"} |= \"%s\"", joinCodeValue);
        final long startNano = start.toEpochMilli() * 1_000_000L;
        final long endNano = end.toEpochMilli() * 1_000_000L;
        final String encodedUri = String.format(
                "/loki/api/v1/query_range?query=%s&start=%d&end=%d&limit=%d",
                URLEncoder.encode(logqlQuery, StandardCharsets.UTF_8),
                startNano, endNano, LOG_LIMIT
        );

        try {
            final String response = restClient.get()
                    .uri(encodedUri)
                    .retrieve()
                    .body(String.class);
            return ToolExecutionResult.ok(TOOL_NAME, response != null ? response : "로그 없음");
        } catch (RestClientException e) {
            log.warn("[ZzolBot] Loki 조회 실패. joinCode={}", joinCodeValue, e);
            return ToolExecutionResult.fail(TOOL_NAME, "Loki 조회 실패: " + e.getMessage());
        }
    }

    private int parseLookBackMinutes(String since) {
        if (since == null || since.isBlank()) {
            return DEFAULT_LOOK_BACK_MINUTES;
        }
        if (since.endsWith("h")) {
            return Integer.parseInt(since.replace("h", "")) * 60;
        }
        if (since.endsWith("m")) {
            return Integer.parseInt(since.replace("m", ""));
        }
        return DEFAULT_LOOK_BACK_MINUTES;
    }
}
