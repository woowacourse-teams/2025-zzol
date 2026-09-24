package coffeeshout.global.redis.pubsub;

import coffeeshout.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NotificationChannelErrorCode implements ErrorCode {
    NOTIFICATION_SERIALIZATION_FAILED(500, "알림 페이로드 직렬화에 실패했습니다."),
    ;

    private final int statusCode;
    private final String message;

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
