package coffeeshout.global.websocket;

import coffeeshout.global.websocket.ui.WebSocketResponse;
import io.micrometer.observation.annotation.Observed;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingSimpMessagingTemplate {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameRecoveryService gameRecoveryService;

    // destination 패턴: /topic/room/{joinCode} 또는 /topic/room/{joinCode}/...
    private static final Pattern ROOM_DESTINATION_PATTERN = Pattern.compile("/topic/room/([^/]+)(?:/.*)?");

    @Observed(name = "websocket.send")
    public void convertAndSend(String destination, Object payload) {
        // WebSocketResponse인 경우에만 복구 저장 처리
        if (payload instanceof WebSocketResponse<?> response) {
            String joinCode = extractJoinCode(destination);

            if (joinCode != null) {
                // 1. messageId 생성 (Hash 기반)
                String messageId = gameRecoveryService.generateMessageId(destination, response);

                // 2. ID 추가된 새 response 생성
                WebSocketResponse<?> responseWithId = response.withId(messageId);

                // 3. Recovery Stream에 저장 (Lua Script로 중복 방지)
                final String streamId = gameRecoveryService.save(joinCode, destination, responseWithId, messageId);
                if (streamId == null) {
                    log.warn("복구 메시지 저장 실패: joinCode={}, destination={}, messageId={}", joinCode, destination, messageId);
                }

                // 4. 웹소켓 전송 (ID 포함)
                messagingTemplate.convertAndSend(destination, responseWithId);
                return;
            }
        }

        // 복구 대상이 아닌 경우 그대로 전송
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Observed(name = "websocket.send.toUser")
    public void convertAndSendToUser(String sessionId, String destination, Object payload) {
        // 개인 메시지는 복구 대상 제외
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload);
    }

    @Observed(name = "websocket.send.error")
    public void convertAndSendError(String sessionId, String errorMessage) {
        // 에러 메시지는 복구 대상 제외
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", WebSocketResponse.error(errorMessage));
    }

    /**
     * destination에서 joinCode 추출
     *
     * @param destination 예: "/topic/room/ABC123/gameState"
     * @return joinCode 예: "ABC123", 추출 실패 시 null
     */
    private String extractJoinCode(String destination) {
        Matcher matcher = ROOM_DESTINATION_PATTERN.matcher(destination);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
}
