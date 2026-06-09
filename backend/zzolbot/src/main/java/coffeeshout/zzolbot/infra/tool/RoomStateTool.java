package coffeeshout.zzolbot.infra.tool;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotTool;
import coffeeshout.room.domain.Room;
import coffeeshout.room.application.service.RoomQueryService;
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
public class RoomStateTool implements ZzolBotTool {

    static final String TOOL_NAME = "room_state";

    private final RoomQueryService roomQueryService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "joinCode로 방의 현재 상태를 조회한다. " +
                "방 상태(READY/PLAYING/SCORE_BOARD/ROULETTE/DONE), 호스트, 플레이어 목록, " +
                "대기 중인 미니게임 큐, 완료된 미니게임 이력을 반환한다.";
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
    public ToolExecutionResult execute(Map<String, Object> params, AskContext ctx) {
        if (!(params.get("joinCode") instanceof String joinCodeValue)) {
            return ToolExecutionResult.fail(TOOL_NAME, "joinCode 파라미터가 누락되었거나 올바르지 않습니다.");
        }
        try {
            final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCodeValue));
            return ToolExecutionResult.ok(TOOL_NAME, objectMapper.writeValueAsString(buildSummary(room)));
        } catch (BusinessException e) {
            return ToolExecutionResult.fail(TOOL_NAME, "방을 찾을 수 없습니다: " + joinCodeValue);
        } catch (JsonProcessingException e) {
            log.warn("[ZzolBot] room_state 직렬화 실패. joinCode={}", joinCodeValue, e);
            return ToolExecutionResult.fail(TOOL_NAME, "방 상태 직렬화 실패");
        }
    }

    private Map<String, Object> buildSummary(Room room) {
        final Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("joinCode", room.getJoinCode().getValue());
        summary.put("roomState", room.getRoomState().name());
        summary.put("host", room.getHost().getName().value());
        summary.put("playerCount", room.getPlayers().size());
        summary.put("players", room.getPlayers().stream()
                .map(p -> p.getName().value())
                .toList());
        summary.put("pendingMiniGames", room.getMiniGames().stream()
                .map(g -> g.getMiniGameType().name())
                .toList());
        summary.put("finishedMiniGames", room.getFinishedGames().stream()
                .map(g -> g.getMiniGameType().name())
                .toList());
        return summary;
    }
}
