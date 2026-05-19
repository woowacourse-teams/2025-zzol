package coffeeshout.room.infra.nickname.persistence;

import coffeeshout.room.domain.audit.AiConfidence;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
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
@Table(name = "player_name_audit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerNameAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String playerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PlayerNameAuditStatus status;

    @Column(precision = 3, scale = 2)
    private AiConfidence confidence;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant auditedAt;

    public PlayerNameAuditEntity(String playerName) {
        this.playerName = playerName;
        this.status = PlayerNameAuditStatus.UNAUDITED;
        this.createdAt = Instant.now();
    }

    public void updateStatus(PlayerNameAuditStatus status) {
        this.status = status;
    }

    public void complete(PlayerNameAuditStatus status, AiConfidence confidence, String reason) {
        this.status = status;
        this.confidence = confidence;
        this.reason = reason;
        this.auditedAt = Instant.now();
    }
}
