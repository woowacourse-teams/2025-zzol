package coffeeshout.bombrelay.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum BombRelayGameErrorCode implements ErrorCode {

    NOT_PLAYING_STATE("게임이 진행 중이 아닙니다."),
    NOT_CURRENT_TURN("현재 차례가 아닙니다."),
    INVALID_FIRST_CHAR("이전 단어의 마지막 글자로 시작해야 합니다."),
    ALREADY_USED_WORD("이미 사용된 단어입니다."),
    WORD_NOT_FOUND("사전에 존재하지 않는 단어입니다."),
    SINGLE_CHAR_WORD("한 글자 단어는 사용할 수 없습니다."),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
