package coffeeshout.report.exception;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ReportErrorCode implements ErrorCode {

    REPORT_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "신고는 일정 시간 내 제한된 횟수만 제출할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
