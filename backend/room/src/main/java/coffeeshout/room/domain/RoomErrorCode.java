package coffeeshout.room.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public enum RoomErrorCode implements ErrorCode {

    JOIN_CODE_NULL(400, "참여 코드는 null일 수 없습니다."),
    JOIN_CODE_ILLEGAL_LENGTH(400, "코드는 4자리여야 합니다."),
    JOIN_CODE_ILLEGAL_CHARACTER(400, "허용되지 않는 문자가 포함되어 있습니다."),
    PLAYER_NAME_BLANK(400, "이름은 공백일 수 없습니다."),
    PLAYER_NAME_TOO_LONG(400, "이름은 10자 이하여야 합니다."),
    PLAYER_NAME_CONTAINS_PROFANITY(400, "비속어가 포함된 닉네임은 사용할 수 없습니다."),
    PLAYER_NAME_GENERATION_FAILED(500, "닉네임 생성 실패: 최대 재시도 횟수를 초과했습니다."),
    ROOM_NOT_READY_TO_JOIN(409, "READY 상태에서만 참여 가능합니다."),
    ROOM_FULL(409, "방에는 최대 9명만 입장 가능합니다."),
    DUPLICATE_PLAYER_NAME(409, "중복된 닉네임은 들어올 수 없습니다."),
    NO_EXIST_PLAYER(404, "플레이어가 존재하지 않습니다."),
    NO_EXIST_PLAYER_NAME_AUDIT(404, "플레이어 검열 항목을 찾을 수 없습니다."),
    NOT_HOST(403, "호스트만 수행할 수 있습니다."),
    INVALID_ADJUSTMENT_WEIGHT(400, "가중치는 0.1 이상 0.9 이하여야 합니다."),
    ROOM_NOT_READY_TO_UPDATE(409, "READY 상태에서만 설정을 변경할 수 있습니다."),
    INSUFFICIENT_PLAYER_COUNT(400, "플레이어는 2명 이상이어야 합니다."),
    INVALID_ROUND_COUNT(400, "라운드 수는 양수여야 합니다."),
    ROOM_NOT_FOUND(404, "존재하지 않는 방입니다."),
    INVITER_NOT_IN_ROOM(403, "방에 참여 중인 사용자만 초대할 수 있습니다."),
    ;

    private final int httpStatusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
