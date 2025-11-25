package coffeeshout.cardgame.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CardGameErrorCode implements ErrorCode {
    NOT_PLAYING_STATE("현재 게임이 진행중인 상태가 아닙니다."),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
