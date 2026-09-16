package coffeeshout.admin.audit.ui.response;

import coffeeshout.admin.audit.domain.AdminAuditLog;
import coffeeshout.admin.audit.domain.AdminAuditResult;
import java.time.Instant;

/**
 * @param action   {@code POST /admin/api/accounts} 형태. 실제 id 가 아니라 매핑 패턴이다.
 * @param targetId 조치 대상. 경로 변수를 이어 붙인 값이라 여럿이면 쉼표로 구분된다.
 * @param detail   실패한 경우 예외 요약. 성공이면 비어 있다.
 */
public record AdminAuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        String targetType,
        String targetId,
        String detail,
        AdminAuditResult result,
        Instant createdAt) {

    public static AdminAuditLogResponse from(AdminAuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getActorEmail(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetail(),
                log.getResult(),
                log.getCreatedAt());
    }
}
