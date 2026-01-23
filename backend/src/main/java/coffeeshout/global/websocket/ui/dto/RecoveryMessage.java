package coffeeshout.global.websocket.ui.dto;

import coffeeshout.global.websocket.ui.WebSocketResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 복구용 메시지 DTO
 *
 * @param streamId Redis Stream Entry ID (복구 요청 시 lastId로 사용)
 * @param destination 원래 웹소켓 destination (프론트 라우팅용)
 * @param response WebSocketResponse 객체
 * @param timestamp 메시지 생성 시간 (epoch millis)
 */
@Slf4j
public record RecoveryMessage(
        String streamId,
        String destination,
        WebSocketResponse<?> response,
        long timestamp
) {
}
