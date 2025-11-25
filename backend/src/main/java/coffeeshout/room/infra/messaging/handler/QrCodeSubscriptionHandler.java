package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.exception.custom.NotExistElementException;
import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.PathMatcher;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class QrCodeSubscriptionHandler {

    private static final String QR_CODE_TOPIC_PATTERN = "/topic/room/{joinCode:.{4}}/qr-code";
    private static final long SUBSCRIPTION_DELAY_MS = 200;

    private final RoomService roomService;
    private final LoggingSimpMessagingTemplate messagingTemplate;
    private final PathMatcher pathMatcher;

    @EventListener
    public void handleSubscribeQrCodeStatus(SessionSubscribeEvent event) {
        final SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

        final String destination = headerAccessor.getDestination();

        if (destination != null && pathMatcher.match(QR_CODE_TOPIC_PATTERN, destination)) {
            final String sessionId = headerAccessor.getSessionId();
            log.info("QR 코드 상태 구독 이벤트 감지: sessionId={}, destination={}",
                    sessionId, destination);

            final Map<String, String> variables = pathMatcher.extractUriTemplateVariables(QR_CODE_TOPIC_PATTERN, destination);
            final String joinCode = variables.get("joinCode");

            if (sessionId == null) {
                log.warn("세션 ID가 null입니다. QR 코드 상태를 전송할 수 없습니다: joinCode={}", joinCode);
                return;
            }

            final QrCodeStatusResponse qrCodeStatus = roomService.getQrCodeStatus(joinCode);

            CompletableFuture.delayedExecutor(SUBSCRIPTION_DELAY_MS, TimeUnit.MILLISECONDS)
                    .execute(() -> sendQrCodeStatus(destination, sessionId, joinCode, qrCodeStatus));
        }
    }

    private void sendQrCodeStatus(String destination, String sessionId, String joinCode, QrCodeStatusResponse qrCodeStatus) {
        try {
            messagingTemplate.convertAndSend(
                    destination,
                    WebSocketResponse.success(qrCodeStatus)
            );

            log.info("QR 코드 구독 시 현재 상태 전송 완료: sessionId={}, joinCode={}, status={}",
                    sessionId, joinCode, qrCodeStatus.status());
        } catch (NotExistElementException e) {
            messagingTemplate.convertAndSendError(sessionId, "해당 방이 존재하지 않습니다.");
        } catch (Exception e) {
            log.error("QR 코드 상태 전송 중 오류 발생: sessionId={}, joinCode={}, error={}",
                    sessionId, joinCode, e.getMessage(), e);
        }
    }
}
