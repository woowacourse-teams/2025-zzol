package coffeeshout.zzolbot.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ZzolBotErrorCode implements ErrorCode {

    INVALID_SQL(HttpStatus.BAD_REQUEST, "단일 SELECT 문만 허용됩니다."),
    SQL_TABLE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않은 테이블을 참조하고 있습니다."),
    SQL_COLUMN_BLOCKED(HttpStatus.BAD_REQUEST, "조회가 차단된 컬럼을 포함하고 있습니다."),
    SQL_WILDCARD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "와일드카드(*)는 허용되지 않습니다. 컬럼을 명시해 주세요."),
    SQL_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SQL 실행 중 오류가 발생했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
