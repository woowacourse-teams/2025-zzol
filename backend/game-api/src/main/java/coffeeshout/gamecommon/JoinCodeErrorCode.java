package coffeeshout.gamecommon;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum JoinCodeErrorCode implements ErrorCode {

    JOIN_CODE_NULL(400, "참여 코드는 null일 수 없습니다."),
    JOIN_CODE_ILLEGAL_LENGTH(400, "코드는 4자리여야 합니다."),
    JOIN_CODE_ILLEGAL_CHARACTER(400, "허용되지 않는 문자가 포함되어 있습니다."),
    ;

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
