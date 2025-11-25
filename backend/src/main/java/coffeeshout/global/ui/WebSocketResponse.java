package coffeeshout.global.ui;

public record WebSocketResponse<T>(
        boolean success,
        T data,
        String errorMessage
) {

    public static <T> WebSocketResponse<T> success(T data) {
        return new WebSocketResponse<>(true, data, null);
    }

    public static <T> WebSocketResponse<T> error(String message) {
        return new WebSocketResponse<>(false, null, message);
    }
}
