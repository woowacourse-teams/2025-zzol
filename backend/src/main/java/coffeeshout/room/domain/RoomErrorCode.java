package coffeeshout.room.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum RoomErrorCode implements ErrorCode {

    JOIN_CODE_NULL(HttpStatus.BAD_REQUEST, "참여 코드는 null일 수 없습니다."),
    JOIN_CODE_ILLEGAL_LENGTH(HttpStatus.BAD_REQUEST, "코드는 4자리여야 합니다."),
    JOIN_CODE_ILLEGAL_CHARACTER(HttpStatus.BAD_REQUEST, "허용되지 않는 문자가 포함되어 있습니다."),
    PLAYER_NAME_BLANK(HttpStatus.BAD_REQUEST, "이름은 공백일 수 없습니다."),
    PLAYER_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "이름은 10자 이하여야 합니다."),
    PLAYER_NAME_CONTAINS_PROFANITY(HttpStatus.BAD_REQUEST, "비속어가 포함된 닉네임은 사용할 수 없습니다."),
    PLAYER_NAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "닉네임 생성 실패: 최대 재시도 횟수를 초과했습니다."),
    ROOM_NOT_READY_TO_JOIN(HttpStatus.CONFLICT, "READY 상태에서만 참여 가능합니다."),
    ROOM_FULL(HttpStatus.CONFLICT, "방에는 최대 9명만 입장 가능합니다."),
    DUPLICATE_PLAYER_NAME(HttpStatus.CONFLICT, "중복된 닉네임은 들어올 수 없습니다."),
    NO_EXIST_PLAYER(HttpStatus.NOT_FOUND, "플레이어가 존재하지 않습니다."),
    NO_EXIST_PLAYER_NAME_AUDIT(HttpStatus.NOT_FOUND, "플레이어 검열 항목을 찾을 수 없습니다."),
    NOT_HOST(HttpStatus.FORBIDDEN, "호스트만 수행할 수 있습니다."),
    INVALID_ADJUSTMENT_WEIGHT(HttpStatus.BAD_REQUEST, "가중치는 0.1 이상 0.9 이하여야 합니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
