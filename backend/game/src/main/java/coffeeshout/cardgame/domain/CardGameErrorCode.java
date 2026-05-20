package coffeeshout.cardgame.domain;

import coffeeshout.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CardGameErrorCode implements ErrorCode {

    NOT_PLAYING_STATE(HttpStatus.CONFLICT, "현재 게임이 진행중인 상태가 아닙니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
