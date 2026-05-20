package coffeeshout.blockstacking.domain;

import coffeeshout.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BlockStackingGameErrorCode implements ErrorCode {

    NOT_PLAYING_STATE(HttpStatus.CONFLICT, "현재 게임이 진행중인 상태가 아닙니다."),
    PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이어를 찾을 수 없습니다."),
    INVALID_PROGRESS(HttpStatus.BAD_REQUEST, "유효하지 않은 진행 데이터입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
