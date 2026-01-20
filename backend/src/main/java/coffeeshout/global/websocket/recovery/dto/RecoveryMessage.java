package coffeeshout.global.websocket.recovery.dto;

import coffeeshout.global.websocket.ui.WebSocketResponse;

/**
 * 복구용 메시지 DTO
 *
 * @param messageId Hash 기반 메시지 ID
 * @param destination 원래 웹소켓 destination (프론트 라우팅용)
 * @param response WebSocketResponse 객체
 * @param timestamp 메시지 생성 시간 (epoch millis)
 */
public record RecoveryMessage(
        String messageId,
        String destination,
        WebSocketResponse<?> response,
        long timestamp
) {
}
