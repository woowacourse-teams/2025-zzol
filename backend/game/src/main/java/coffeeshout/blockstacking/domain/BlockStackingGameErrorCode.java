package coffeeshout.blockstacking.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum BlockStackingGameErrorCode implements ErrorCode {

    NOT_PLAYING_STATE(409, "현재 게임이 진행중인 상태가 아닙니다."),
    PLAYER_NOT_FOUND(404, "플레이어를 찾을 수 없습니다."),
    INVALID_PROGRESS(400, "유효하지 않은 진행 데이터입니다."),
    ;

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
