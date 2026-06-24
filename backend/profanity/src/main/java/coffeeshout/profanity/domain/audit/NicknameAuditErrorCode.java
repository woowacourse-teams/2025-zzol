package coffeeshout.profanity.domain.audit;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NicknameAuditErrorCode implements ErrorCode {

    AI_CALL_FAILED("AUDIT_001", "닉네임 검열 AI 호출에 실패했습니다.", 500),
    AI_EMPTY_RESPONSE("AUDIT_002", "닉네임 검열 AI가 빈 응답을 반환했습니다.", 500),
    AI_RESPONSE_PARSE_FAILED("AUDIT_003", "닉네임 검열 AI 응답 파싱에 실패했습니다.", 500),
    PROMPT_BUILD_FAILED("AUDIT_004", "닉네임 검열 프롬프트 생성에 실패했습니다.", 500),
    AUDIT_NOT_FOUND("AUDIT_005", "검열 항목을 찾을 수 없습니다.", 404),
    AI_RATE_LIMIT_EXHAUSTED("AUDIT_006", "모든 Gemini 모델의 요청 한도를 초과했습니다.", 503);

    private final String code;
    private final String message;
    private final int statusCode;
}
