package coffeeshout.user.exception;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public enum UserErrorCode implements ErrorCode {

    USER_CODE_GENERATION_FAILED(500, "사용자 식별 코드 생성에 실패했습니다."),
    USER_CODE_INVALID(400, "유효하지 않은 사용자 식별 코드입니다."),
    USER_NOT_FOUND(404, "존재하지 않는 회원입니다."),
    NICKNAME_BLANK(400, "닉네임은 공백일 수 없습니다."),
    NICKNAME_TOO_LONG(400, "닉네임은 10자 이하여야 합니다."),
    NICKNAME_INVALID(400, "허용되지 않는 닉네임입니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(401, "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(401, "존재하지 않는 리프레시 토큰입니다."),
    OAUTH_PROVIDER_NOT_SUPPORTED(400, "지원하지 않는 OAuth 제공자입니다."),
    OAUTH_PROVIDER_ERROR(502, "OAuth 제공자로부터 필수 정보를 가져올 수 없습니다."),
    OAUTH_CODE_NOT_FOUND(401, "유효하지 않거나 만료된 인증 코드입니다.");

    private final int httpStatusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
