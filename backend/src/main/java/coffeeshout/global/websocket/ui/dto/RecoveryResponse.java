package coffeeshout.global.websocket.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 복구 API 응답 DTO
 *
 * @param success 성공 여부
 * @param messageCount 복구된 메시지 개수
 * @param errorMessage 에러 메시지 (실패 시)
 * @param messages 복구된 메시지 리스트
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "복구 API 응답")
public record RecoveryResponse(
        @Schema(description = "성공 여부", example = "true")
        boolean success,

        @Schema(description = "복구된 메시지 개수", example = "5")
        int messageCount,

        @Schema(description = "에러 메시지 (실패 시)", example = "웹소켓 미연결")
        String errorMessage,

        @Schema(description = "복구된 메시지 리스트")
        List<RecoveryMessage> messages
) {

    public static RecoveryResponse success(List<RecoveryMessage> messages) {
        return new RecoveryResponse(true, messages.size(), null, messages);
    }

    public static RecoveryResponse error(String errorMessage) {
        return new RecoveryResponse(false, 0, errorMessage, List.of());
    }
}
