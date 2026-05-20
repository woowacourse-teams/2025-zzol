package coffeeshout.room.infra.nickname.persistence;

import coffeeshout.room.domain.audit.AiConfidence;
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
@Table(name = "player_name_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerNameFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String playerName;

    @Column(nullable = false)
    private boolean aiFlagged;

    @Column(nullable = false, precision = 3, scale = 2)
    private AiConfidence aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OperatorDecision operatorDecision;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    public PlayerNameFeedbackEntity(String playerName, boolean aiFlagged, AiConfidence aiConfidence,
                                    OperatorDecision operatorDecision, String reason) {
        this.playerName = playerName;
        this.aiFlagged = aiFlagged;
        this.aiConfidence = aiConfidence != null ? aiConfidence : AiConfidence.UNKNOWN;
        this.operatorDecision = operatorDecision;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public enum OperatorDecision {
        ALLOWED, BLOCKED
    }
}
