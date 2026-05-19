package coffeeshout.laddergame.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LadderGameErrorCode implements ErrorCode {

    PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이어를 찾을 수 없습니다."),
    INVALID_POLE_INDEX(HttpStatus.INTERNAL_SERVER_ERROR, "유효하지 않은 기둥 인덱스입니다."),
    INVALID_PLAYER_COUNT(HttpStatus.BAD_REQUEST, "플레이어 수는 1 이상이어야 합니다."),
    ALREADY_DREW(HttpStatus.CONFLICT, "이미 선을 그은 플레이어입니다."),
    INVALID_STATE_TRANSITION(HttpStatus.INTERNAL_SERVER_ERROR, "허용되지 않는 상태 전환입니다."),
    PATH_NOT_TRACED(HttpStatus.INTERNAL_SERVER_ERROR, "경로 추적이 완료되지 않았습니다."),
    INVALID_SEGMENT_INDEX(HttpStatus.BAD_REQUEST, "유효하지 않은 선 위치입니다."),
    INVALID_LINE_ROW(HttpStatus.INTERNAL_SERVER_ERROR, "유효하지 않은 행 번호입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
