package coffeeshout.wormgame.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum WormGameErrorCode implements ErrorCode {
    NOT_PLAYING_STATE(409, "게임이 진행 중이 아닙니다."),
    INVALID_STEERING(400, "유효하지 않은 조향 각도입니다."),
    NOT_EXIST_WORM(404, "해당 플레이어의 지렁이가 존재하지 않습니다."),
    ;

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
