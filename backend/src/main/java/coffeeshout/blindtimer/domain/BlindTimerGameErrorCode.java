package coffeeshout.blindtimer.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum BlindTimerGameErrorCode implements ErrorCode {

    NOT_PLAYING_STATE("게임이 진행 중이 아닙니다."),
    ALREADY_STOPPED("이미 멈춘 플레이어입니다."),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
