package coffeeshout.room.domain.audit;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlayerNameAuditErrorCode implements ErrorCode {

    AI_CALL_FAILED("AUDIT_001", "닉네임 검열 AI 호출에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_EMPTY_RESPONSE("AUDIT_002", "닉네임 검열 AI가 빈 응답을 반환했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_RESPONSE_PARSE_FAILED("AUDIT_003", "닉네임 검열 AI 응답 파싱에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
