package coffeeshout.admin.account.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AdminAccountErrorCode implements ErrorCode {
    NOT_ADMIN(403, "관리자 허용목록에 없는 계정입니다."),
    INVALID_ADMIN_EMAIL(400, "관리자 이메일 형식이 올바르지 않습니다."),
    ADMIN_ACCOUNT_ALREADY_EXISTS(409, "이미 등록된 관리자입니다."),
    ADMIN_ACCOUNT_NOT_FOUND(404, "존재하지 않는 관리자입니다."),
    CANNOT_REMOVE_SELF(400, "자기 자신은 삭제할 수 없습니다."),
    ADMIN_TOKEN_INVALID(401, "유효하지 않은 관리자 토큰입니다."),
    ADMIN_TOKEN_EXPIRED(401, "만료된 관리자 토큰입니다."),
    ADMIN_REFRESH_TOKEN_INVALID(401, "다시 로그인해 주세요."),
    ADMIN_ORIGIN_NOT_ALLOWED(403, "허용되지 않은 출처의 요청입니다."),
    GOOGLE_ID_TOKEN_INVALID(401, "구글 인증에 실패했습니다."),
    GOOGLE_EMAIL_UNVERIFIED(401, "이메일이 검증되지 않은 구글 계정입니다.");

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
