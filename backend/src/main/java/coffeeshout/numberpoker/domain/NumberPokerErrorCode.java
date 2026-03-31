package coffeeshout.numberpoker.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NumberPokerErrorCode implements ErrorCode {
    INVALID_CARD_VALUE(HttpStatus.BAD_REQUEST, "카드 값은 1~10이어야 합니다."),
    INVALID_ROUND_COUNT(HttpStatus.BAD_REQUEST, "라운드 수는 1~5이어야 합니다."),
    ALREADY_FOLDED(HttpStatus.CONFLICT, "이미 폴드한 플레이어입니다."),
    INVALID_PHASE_ACTION(HttpStatus.CONFLICT, "현재 페이즈에서 허용되지 않는 액션입니다."),
    PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 플레이어를 찾을 수 없습니다."),
    ROUND_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "진행 중인 라운드가 없습니다."),
    DECK_EMPTY(HttpStatus.INTERNAL_SERVER_ERROR, "덱에 카드가 부족합니다."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 넘버포커 게임을 찾을 수 없습니다."),
    NOT_HOST(HttpStatus.FORBIDDEN, "호스트만 이 작업을 수행할 수 있습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
