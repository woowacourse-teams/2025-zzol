package coffeeshout.speedtouch.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SpeedTouchGameErrorCode implements ErrorCode {

    NOT_PLAYING_STATE("게임이 진행 중이 아닙니다."),
    INVALID_TOUCH_NUMBER("터치 순서가 올바르지 않습니다."),
    ALREADY_FINISHED("이미 완주한 플레이어입니다."),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
