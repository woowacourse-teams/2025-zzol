package coffeeshout.profanity.domain.audit;

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

@Entity
@Table(name = "player_name_audit") // :room에서 이전 시 기존 테이블 유지 — 스키마 마이그레이션 없이 호환
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NicknameAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_name", nullable = false, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NicknameAuditStatus status;

    @Column(precision = 3, scale = 2)
    private AiConfidence confidence;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant auditedAt;

    public NicknameAudit(String nickname) {
        this.nickname = nickname;
        this.status = NicknameAuditStatus.UNAUDITED;
        this.createdAt = Instant.now();
    }

    public void updateStatus(NicknameAuditStatus status) {
        this.status = status;
    }

    public void complete(NicknameAuditStatus status, AiConfidence confidence, String reason) {
        this.status = status;
        this.confidence = confidence;
        this.reason = reason;
        this.auditedAt = Instant.now();
    }
}
