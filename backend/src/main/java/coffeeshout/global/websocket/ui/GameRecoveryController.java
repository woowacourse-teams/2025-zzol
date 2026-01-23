package coffeeshout.global.websocket.ui;

import coffeeshout.global.websocket.GameRecoveryService;
import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.global.websocket.ui.dto.RecoveryMessage;
import coffeeshout.global.websocket.ui.dto.RecoveryResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            @PathVariable @NotBlank String joinCode,
            @RequestParam @NotBlank String playerName,
            @RequestParam @NotBlank String lastId
    ) {
        try {
            // 2. 웹소켓 연결 확인
            if (!stompSessionManager.hasSessionId(joinCode, playerName)) {
                log.warn("복구 요청 실패: 웹소켓 미연결 - joinCode={}, playerName={}", joinCode, playerName);
                return ResponseEntity.badRequest()
                        .body(RecoveryResponse.error("웹소켓 미연결"));
            }

            // 3. 복구 메시지 조회
            final List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, lastId);

            log.info("메시지 복구 완료: joinCode={}, playerName={}, lastId={}, count={}",
                    joinCode, playerName, lastId, messages.size());

            return ResponseEntity.ok(RecoveryResponse.success(messages));

        } catch (Exception e) {
            log.error("메시지 복구 실패: joinCode={}, playerName={}, lastId={}",
                    joinCode, playerName, lastId, e);
            return ResponseEntity.internalServerError()
                    .body(RecoveryResponse.error("메세지 복구 실패"));
        }
    }
}
