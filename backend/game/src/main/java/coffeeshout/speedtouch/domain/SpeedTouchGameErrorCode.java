package coffeeshout.speedtouch.domain;

import coffeeshout.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum SpeedTouchGameErrorCode implements ErrorCode {

    NOT_PLAYING_STATE(HttpStatus.CONFLICT, "게임이 진행 중이 아닙니다."),
    INVALID_TOUCH_NUMBER(HttpStatus.CONFLICT, "터치 순서가 올바르지 않습니다."),
    ALREADY_FINISHED(HttpStatus.CONFLICT, "이미 완주한 플레이어입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
