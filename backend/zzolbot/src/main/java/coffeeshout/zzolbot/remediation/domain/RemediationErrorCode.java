package coffeeshout.zzolbot.remediation.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RemediationErrorCode implements ErrorCode {

    GITHUB_TOKEN_MISSING(500, "자동 수정 디스패치 토큰(ZZOL_BOT_GH_DISPATCH_TOKEN)이 설정되지 않았습니다."),
    GITHUB_DISPATCH_FAILED(502, "GitHub 자동 수정 워크플로우 디스패치에 실패했습니다."),
    ;

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
