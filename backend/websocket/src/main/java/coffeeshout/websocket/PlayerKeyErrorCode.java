package coffeeshout.websocket;

import coffeeshout.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlayerKeyErrorCode implements ErrorCode {

    INVALID_PLAYER_KEY_FORMAT(HttpStatus.BAD_REQUEST, "플레이어 키 형식이 잘못되었습니다."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증되지 않은 연결입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
