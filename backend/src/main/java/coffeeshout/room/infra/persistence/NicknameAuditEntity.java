package coffeeshout.room.infra.persistence;

import coffeeshout.room.domain.audit.NicknameAuditStatus;
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
@Table(name = "nickname_audit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NicknameAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NicknameAuditStatus status;

    @Column(precision = 3, scale = 2)
    private Double confidence;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant auditedAt;

    public NicknameAuditEntity(String nickname) {
        this.nickname = nickname;
        this.status = NicknameAuditStatus.UNAUDITED;
        this.createdAt = Instant.now();
    }

    public void complete(NicknameAuditStatus status, double confidence, String reason) {
        this.status = status;
        this.confidence = confidence;
        this.reason = reason;
        this.auditedAt = Instant.now();
    }
}
