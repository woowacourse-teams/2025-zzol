package coffeeshout.blindtimer.domain;

import coffeeshout.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum BlindTimerGameErrorCode implements ErrorCode {

    NOT_PLAYING_STATE(HttpStatus.CONFLICT, "게임이 진행 중이 아닙니다."),
    ALREADY_STOPPED(HttpStatus.CONFLICT, "이미 멈춘 플레이어입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
