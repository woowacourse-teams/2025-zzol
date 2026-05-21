package coffeeshout.minigame.domain;

import coffeeshout.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GamerErrorCode implements ErrorCode {

    UNAUTHORIZED_GAMER(HttpStatus.FORBIDDEN, "게임 액션 권한이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
