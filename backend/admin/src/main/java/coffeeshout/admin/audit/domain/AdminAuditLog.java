package coffeeshout.admin.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 조치 기록. append-only 다. 수정도 삭제도 하지 않는다.
 *
 * <p>요청 본문은 담지 않는다. 로그인 요청에 구글 ID 토큰이 실려 있어서, 본문을 통째로 남기면
 * 감사 테이블이 곧 토큰 저장소가 된다. 남기는 것은 누가, 무엇을, 어디에, 결과가 어땠는지다.
 */
@Entity
@Table(name = "admin_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    private static final int MAX_ACTION = 100;
    private static final int MAX_TARGET_TYPE = 50;
    private static final int MAX_TARGET_ID = 255;
    private static final int MAX_ACTOR = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 실행한 관리자 이메일. 인증 전 요청(로그인 시도)은 {@code anonymous}. */
    @Column(name = "actor_email", nullable = false, length = MAX_ACTOR)
    private String actorEmail;

    /** {@code POST /admin/api/accounts} 형태. */
    @Column(nullable = false, length = MAX_ACTION)
    private String action;

    @Column(name = "target_type", length = MAX_TARGET_TYPE)
    private String targetType;

    @Column(name = "target_id", length = MAX_TARGET_ID)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminAuditResult result;

    @Column(nullable = false)
    private Instant createdAt;

    public static AdminAuditLog of(
            String actorEmail,
            String action,
            String targetType,
            String targetId,
            String detail,
            AdminAuditResult result,
            Instant now) {
        final AdminAuditLog log = new AdminAuditLog();
        // 길이 초과로 감사 기록이 통째로 유실되면 안 된다. 자르더라도 남기는 쪽을 택한다.
        log.actorEmail = truncate(blankToAnonymous(actorEmail), MAX_ACTOR);
        log.action = truncate(action, MAX_ACTION);
        log.targetType = truncate(targetType, MAX_TARGET_TYPE);
        log.targetId = truncate(targetId, MAX_TARGET_ID);
        log.detail = detail;
        log.result = result;
        log.createdAt = now;
        return log;
    }

    private static String blankToAnonymous(String actorEmail) {
        return (actorEmail == null || actorEmail.isBlank()) ? "anonymous" : actorEmail;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
