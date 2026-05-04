package coffeeshout.global.zzolbot.infra.tool;

import coffeeshout.global.outbox.OutboxEvent;
import coffeeshout.global.outbox.OutboxStatus;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import coffeeshout.global.zzolbot.infra.ZzolBotOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxTool implements ZzolBotTool {

    static final String TOOL_NAME = "outbox_events";

    private final ZzolBotOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "joinCode와 관련된 Outbox 이벤트 중 PENDING(재시도 대기) 또는 DEAD_LETTER(최종 실패) 상태의 이벤트를 조회한다. " +
                "이벤트 유실이나 Redis Stream 발행 실패를 진단할 때 사용한다.";
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
    @Transactional(readOnly = true)
    public ToolExecutionResult execute(Map<String, Object> params) {
        final String joinCodeValue = (String) params.get("joinCode");
        if (joinCodeValue == null || !joinCodeValue.matches("[A-Z0-9]{4}")) {
            return ToolExecutionResult.fail(TOOL_NAME, "유효하지 않은 joinCode 형식: " + joinCodeValue);
        }
        try {
            final List<OutboxEvent> events = outboxRepository.findRecentByStatusInAndPayloadContaining(
                    List.of(OutboxStatus.PENDING, OutboxStatus.DEAD_LETTER),
                    "%" + joinCodeValue + "%"
            );
            final List<Map<String, Object>> summaries = events.stream()
                    .map(this::buildEventSummary)
                    .toList();
            return ToolExecutionResult.ok(TOOL_NAME, objectMapper.writeValueAsString(summaries));
        } catch (JsonProcessingException e) {
            log.warn("[ZzolBot] outbox_events 직렬화 실패. joinCode={}", joinCodeValue, e);
            return ToolExecutionResult.fail(TOOL_NAME, "Outbox 이벤트 직렬화 실패");
        } catch (Exception e) {
            log.warn("[ZzolBot] outbox_events 조회 실패. joinCode={}", joinCodeValue, e);
            return ToolExecutionResult.fail(TOOL_NAME, "Outbox 이벤트 조회 실패");
        }
    }

    private Map<String, Object> buildEventSummary(OutboxEvent event) {
        final Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", event.getId());
        summary.put("streamKey", event.getStreamKey());
        summary.put("status", event.getStatus().name());
        summary.put("retryCount", event.getRetryCount());
        summary.put("createdAt", event.getCreatedAt().toString());
        summary.put("updatedAt", event.getUpdatedAt().toString());
        return summary;
    }
}
