package coffeeshout.global.websocket.ui;

import coffeeshout.global.websocket.GameRecoveryService;
import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.global.websocket.ui.dto.RecoveryMessage;
import coffeeshout.global.websocket.ui.dto.RecoveryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 웹소켓 메시지 복구 API
 */
@Slf4j
@RestController
@RequestMapping("/api/rooms/{joinCode}/recovery")
@RequiredArgsConstructor
@Validated
public class GameRecoveryController implements GameRecoveryApi {

    private final GameRecoveryService gameRecoveryService;
    private final StompSessionManager stompSessionManager;

    @Override
    @PostMapping
    public ResponseEntity<RecoveryResponse> requestRecovery(
            @PathVariable String joinCode,
            @RequestParam String playerName,
            @RequestParam String lastId
    ) {
        if (!stompSessionManager.hasSessionId(joinCode, playerName)) {
            log.warn("복구 요청 실패: 웹소켓 미연결 - joinCode={}, playerName={}", joinCode, playerName);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(RecoveryResponse.error("웹소켓 미연결"));
        }

        final List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, lastId);

        log.info("메시지 복구 완료: joinCode={}, playerName={}, lastId={}, count={}",
                joinCode, playerName, lastId, messages.size());

        return ResponseEntity.ok(RecoveryResponse.success(messages));
    }
}
