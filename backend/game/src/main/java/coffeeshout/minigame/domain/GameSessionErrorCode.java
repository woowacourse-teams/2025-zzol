package coffeeshout.minigame.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameSessionErrorCode implements ErrorCode {

    NOT_HOST(403, "호스트만 게임 세션을 조작할 수 있습니다."),
    DUPLICATE_GAME(400, "동일한 게임을 중복 선택할 수 없습니다."),
    TOO_MANY_GAMES(400, "선택 가능한 게임은 최대 5개입니다."),
    GAME_IN_PROGRESS(409, "게임 진행 중에는 대기열을 변경할 수 없습니다."),
    NO_PENDING_GAMES(409, "시작할 수 있는 대기 게임이 없습니다."),
    GAME_NOT_FOUND(404, "해당하는 게임이 존재하지 않습니다."),
    ;

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
