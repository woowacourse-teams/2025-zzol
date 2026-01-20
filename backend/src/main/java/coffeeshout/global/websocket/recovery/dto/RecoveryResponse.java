package coffeeshout.global.websocket.recovery.dto;

import java.util.List;

/**
 * 복구 API 응답 DTO
 *
 * @param success 성공 여부
 * @param messageCount 복구된 메시지 개수
 * @param errorMessage 에러 메시지 (실패 시)
 * @param messages 복구된 메시지 리스트
 */
public record RecoveryResponse(
        boolean success,
        int messageCount,
        String errorMessage,
        List<RecoveryMessage> messages
) {

    public static RecoveryResponse success(List<RecoveryMessage> messages) {
        return new RecoveryResponse(true, messages.size(), null, messages);
    }

    public static RecoveryResponse error(String errorMessage) {
        return new RecoveryResponse(false, 0, errorMessage, List.of());
    }
}
