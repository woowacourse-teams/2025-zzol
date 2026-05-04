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
public class TempoTraceTool implements ZzolBotTool {

    static final String TOOL_NAME = "tempo_traces";
    private static final int TRACE_LIMIT = 20;

    private final RestClient restClient;

    public TempoTraceTool(ZzolBotProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(properties.monitoring().tempoUrl()).build();
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "Tempo에서 joinCode 태그로 분산 트레이스를 조회한다. " +
                "특정 방에서 발생한 요청의 전체 흐름과 소요 시간을 확인할 수 있다.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "joinCode", Map.of(
                                "type", "string",
                                "description", "4자리 방 입장 코드"
                        )
                ),
                "required", List.of("joinCode")
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> params) {
        if (!(params.get("joinCode") instanceof String joinCodeValue) || !joinCodeValue.matches("[A-Z0-9]{4}")) {
            return ToolExecutionResult.fail(TOOL_NAME, "유효하지 않은 joinCode 형식");
        }
        try {
            final String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/search")
                            .queryParam("tags", "joinCode=" + joinCodeValue)
                            .queryParam("limit", TRACE_LIMIT)
                            .build())
                    .retrieve()
                    .body(String.class);
            return ToolExecutionResult.ok(TOOL_NAME, response != null ? response : "트레이스 없음");
        } catch (RestClientException e) {
            log.warn("[ZzolBot] Tempo 조회 실패. joinCode={}", joinCodeValue, e);
            return ToolExecutionResult.fail(TOOL_NAME, "Tempo 조회 실패");
        }
    }
}
