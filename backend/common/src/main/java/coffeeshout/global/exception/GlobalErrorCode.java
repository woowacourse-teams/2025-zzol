package coffeeshout.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public enum GlobalErrorCode implements ErrorCode {

    NOT_EXIST(404, "해당 데이터가 존재하지 않습니다."),
    INVALID_STREAM_ID(400, "유효하지 않은 Stream ID 형식입니다."),
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),
    VALIDATION_ERROR(400, "유효하지 않은 요청입니다."),
    CONSTRAINT_VIOLATION(400, "요청 파라미터가 유효하지 않습니다."),
    IP_BLOCKED(429, "비정상적인 접근으로 일시적으로 차단되었습니다."),
    ;

    private final int httpStatusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
