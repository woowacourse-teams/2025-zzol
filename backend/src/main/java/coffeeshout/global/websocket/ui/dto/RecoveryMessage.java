package coffeeshout.global.websocket.ui.dto;

import coffeeshout.global.websocket.ui.WebSocketResponse;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "복구용 메시지 정보")
public record RecoveryMessage(
        @Schema(description = "Redis Stream Entry ID (복구 요청 시 lastId로 사용)", example = "1769155902692-0")
        String streamId,

        @Schema(description = "원래 웹소켓 destination (프론트 라우팅용)", example = "/topic/room/TESTCODE")
        String destination,

        @Schema(description = "WebSocketResponse 객체")
        WebSocketResponse<?> response,

        @Schema(description = "메시지 생성 시간 (epoch millis)", example = "1769155902388")
        long timestamp
) {
}
