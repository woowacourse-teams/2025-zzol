package coffeeshout.laddergame.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum LadderGameErrorCode implements ErrorCode {

    PLAYER_NOT_FOUND(404, "플레이어를 찾을 수 없습니다."),
    INVALID_POLE_INDEX(500, "유효하지 않은 기둥 인덱스입니다."),
    INVALID_PLAYER_COUNT(400, "플레이어 수는 1 이상이어야 합니다."),
    ALREADY_DREW(409, "이미 선을 그은 플레이어입니다."),
    INVALID_STATE_TRANSITION(500, "허용되지 않는 상태 전환입니다."),
    PATH_NOT_TRACED(500, "경로 추적이 완료되지 않았습니다."),
    INVALID_SEGMENT_INDEX(400, "유효하지 않은 선 위치입니다."),
    INVALID_LINE_ROW(500, "유효하지 않은 행 번호입니다."),
    ;

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
