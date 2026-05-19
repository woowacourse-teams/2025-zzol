package coffeeshout.websocket.auth;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoomSessionTokenErrorCode implements ErrorCode {

    ROOM_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "roomToken 헤더가 없습니다."),
    ROOM_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 Room Session Token입니다."),
    ROOM_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 Room Session Token입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
