package coffeeshout.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum GlobalErrorCode implements ErrorCode {

    NOT_EXIST(HttpStatus.NOT_FOUND, "해당 데이터가 존재하지 않습니다."),
    INVALID_STREAM_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 Stream ID 형식입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
    CONSTRAINT_VIOLATION(HttpStatus.BAD_REQUEST, "요청 파라미터가 유효하지 않습니다."),
    IP_BLOCKED(HttpStatus.TOO_MANY_REQUESTS, "비정상적인 접근으로 일시적으로 차단되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
