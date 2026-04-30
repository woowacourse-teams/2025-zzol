package coffeeshout.user.exception;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum UserErrorCode implements ErrorCode {

    USER_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 식별 코드 생성에 실패했습니다."),
    USER_CODE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 식별 코드입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    NICKNAME_BLANK(HttpStatus.BAD_REQUEST, "닉네임은 공백일 수 없습니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "닉네임은 10자 이하여야 합니다."),
    NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "허용되지 않는 닉네임입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "존재하지 않는 리프레시 토큰입니다."),
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAuth 제공자로부터 필수 정보를 가져올 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
