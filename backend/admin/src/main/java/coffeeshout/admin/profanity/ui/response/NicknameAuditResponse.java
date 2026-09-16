package coffeeshout.admin.profanity.ui.response;

import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @param confidence AI 판정 신뢰도. 값이 없으면 {@code UNKNOWN}으로 채운다.
 *                   화면이 null 을 따로 다루게 하면 표에 빈칸이 생긴다.
 */
public record NicknameAuditResponse(
        Long id,
        String nickname,
        NicknameAuditStatus status,
        AiConfidence confidence,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime auditedAt) {

    public static NicknameAuditResponse from(NicknameAudit audit, ZoneId zone) {
        return new NicknameAuditResponse(
                audit.getId(),
                audit.getNickname(),
                audit.getStatus(),
                audit.getConfidence() != null ? audit.getConfidence() : AiConfidence.UNKNOWN,
                audit.getReason() != null ? audit.getReason() : "",
                LocalDateTime.ofInstant(audit.getCreatedAt(), zone),
                audit.getAuditedAt() == null ? null : LocalDateTime.ofInstant(audit.getAuditedAt(), zone));
    }
}
