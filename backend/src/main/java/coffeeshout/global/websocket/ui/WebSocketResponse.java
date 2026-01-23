package coffeeshout.global.websocket.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "웹소켓 공통 응답 포맷")
public record WebSocketResponse<T>(
        @Schema(description = "성공 여부", example = "true")
        boolean success,

        @Schema(description = "응답 데이터 (제네릭)")
        T data,

        @Schema(description = "에러 메시지 (실패 시)", example = "잘못된 요청입니다.")
        String errorMessage,

        @Schema(description = "메시지 ID (복구용)", example = "1234567890abcdef")
        String id
) {

    public static <T> WebSocketResponse<T> success(T data) {
        return new WebSocketResponse<>(true, data, null, null);
    }

    public static <T> WebSocketResponse<T> error(String message) {
        return new WebSocketResponse<>(false, null, message, null);
    }

    /**
     * ID를 추가한 새 WebSocketResponse 생성 (복구용)
     */
    public WebSocketResponse<T> withId(String id) {
        return new WebSocketResponse<>(this.success, this.data, this.errorMessage, id);
    }
}
