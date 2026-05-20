package coffeeshout.minigame.domain;

import coffeeshout.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GameSessionErrorCode implements ErrorCode {

    NOT_HOST(HttpStatus.FORBIDDEN, "호스트만 수행할 수 있습니다."),
    DUPLICATE_GAME(HttpStatus.BAD_REQUEST, "동일한 게임을 중복으로 선택할 수 없습니다."),
    NO_PENDING_GAMES(HttpStatus.CONFLICT, "시작할 게임이 없습니다."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 미니게임이 존재하지 않습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
