package coffeeshout.gamecommon;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게임 모듈 전반에서 공유하는 횡단 에러 코드. 특정 게임에 종속되지 않는 개념(플레이어 식별 등)을 담는다.
 * 게임별 고유 규칙 위반은 각 게임의 {@code *GameErrorCode}를 사용한다.
 */
@RequiredArgsConstructor
@Getter
public enum GameErrorCode implements ErrorCode {

    PLAYER_NOT_FOUND(404, "플레이어가 존재하지 않습니다."),
    ;

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
