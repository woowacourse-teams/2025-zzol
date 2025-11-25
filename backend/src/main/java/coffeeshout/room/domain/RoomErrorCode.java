package coffeeshout.room.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RoomErrorCode implements ErrorCode {

    JOIN_CODE_NULL("참여 코드는 null일 수 없습니다."),
    JOIN_CODE_ILLEGAL_LENGTH("코드는 4자리여야 합니다."),
    JOIN_CODE_ILLEGAL_CHARACTER("허용되지 않는 문자가 포함되어 있습니다."),
    PLAYER_NAME_BLANK("이름은 공백일 수 없습니다."),
    PLAYER_NAME_TOO_LONG("이름은 10자 이하여야 합니다."),
    ROOM_NOT_READY_TO_JOIN("READY 상태에서만 참여 가능합니다."),
    ROOM_FULL("방에는 최대 9명만 입장 가능합니다."),
    DUPLICATE_PLAYER_NAME("중복된 닉네임은 들어올 수 없습니다."),
    NO_EXIST_PLAYER("플레이어가 존재하지 않습니다."),
    QR_CODE_GENERATION_FAILED("QR 코드 생성에 실패했습니다."),
    QR_CODE_UPLOAD_FAILED("QR 코드 업로드에 실패했습니다."),
    QR_CODE_URL_SIGNING_FAILED("QR 코드 URL 생성에 실패했습니다."),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
