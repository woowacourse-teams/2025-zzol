package coffeeshout.friend.exception;

import coffeeshout.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum FriendErrorCode implements ErrorCode {

    CANNOT_FRIEND_SELF(HttpStatus.BAD_REQUEST, "자기 자신에게 친구 요청을 보낼 수 없습니다."),
    FRIEND_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 친구인 사용자입니다."),
    FRIEND_REQUEST_ALREADY_SENT(HttpStatus.CONFLICT, "이미 친구 요청을 보냈거나 받은 상태입니다."),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 친구 요청입니다."),
    FRIEND_REQUEST_INVALID_STATE(HttpStatus.CONFLICT, "처리할 수 없는 상태의 친구 요청입니다."),
    NOT_FRIEND(HttpStatus.FORBIDDEN, "친구 관계가 아닌 사용자입니다."),
    FRIEND_REQUEST_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 친구 요청을 처리할 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
