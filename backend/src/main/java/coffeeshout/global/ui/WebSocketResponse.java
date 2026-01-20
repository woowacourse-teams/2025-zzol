package coffeeshout.global.ui;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSocketResponse<T>(
        boolean success,
        T data,
        String errorMessage,
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
