package coffeeshout.room.infra.auth;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum RoomSessionTokenErrorCode implements ErrorCode {

    ROOM_TOKEN_MISSING(401, "roomToken 헤더가 없습니다."),
    ROOM_TOKEN_INVALID(401, "유효하지 않은 Room Session Token입니다."),
    ROOM_TOKEN_EXPIRED(401, "만료된 Room Session Token입니다.");

    private final int httpStatusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
