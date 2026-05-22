package coffeeshout.report.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public enum ReportErrorCode implements ErrorCode {

    REPORT_RATE_LIMITED(429, "신고는 일정 시간 내 제한된 횟수만 제출할 수 있습니다."),
    INVALID_CLIENT_IP(400, "클라이언트 IP를 확인할 수 없습니다.");

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
