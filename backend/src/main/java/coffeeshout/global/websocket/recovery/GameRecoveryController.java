package coffeeshout.global.websocket.recovery;

import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.global.websocket.recovery.dto.RecoveryMessage;
import coffeeshout.global.websocket.recovery.dto.RecoveryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
public class GameRecoveryController {

    private final GameRecoveryService gameRecoveryService;
    private final StompSessionManager stompSessionManager;

    /**
     * 유실된 메시지 복구 요청
     *
     * @param joinCode 방 코드
     * @param playerName 플레이어 이름
     * @param lastId 클라이언트가 마지막으로 받은 메시지 ID (필수)
     * @return 복구된 메시지 리스트
     */
    @PostMapping
    public ResponseEntity<RecoveryResponse> requestRecovery(
            @PathVariable String joinCode,
            @RequestParam String playerName,
            @RequestParam String lastId
    ) {
        // 1. lastId 필수 검증
        if (lastId == null || lastId.isBlank()) {
            log.warn("복구 요청 실패: lastId 누락 - joinCode={}, playerName={}", joinCode, playerName);
            return ResponseEntity.badRequest()
                    .body(RecoveryResponse.error("lastId is required"));
        }

        try {
            // 2. 웹소켓 연결 확인
            if (!stompSessionManager.hasSessionId(joinCode, playerName)) {
                log.warn("복구 요청 실패: 웹소켓 미연결 - joinCode={}, playerName={}", joinCode, playerName);
                return ResponseEntity.badRequest()
                        .body(RecoveryResponse.error("WebSocket not connected"));
            }

            // 3. 복구 메시지 조회
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, lastId);

            log.info("메시지 복구 완료: joinCode={}, playerName={}, lastId={}, count={}",
                    joinCode, playerName, lastId, messages.size());

            return ResponseEntity.ok(RecoveryResponse.success(messages));

        } catch (Exception e) {
            log.error("메시지 복구 실패: joinCode={}, playerName={}, lastId={}",
                    joinCode, playerName, lastId, e);
            return ResponseEntity.internalServerError()
                    .body(RecoveryResponse.error("Recovery failed: " + e.getMessage()));
        }
    }
}
