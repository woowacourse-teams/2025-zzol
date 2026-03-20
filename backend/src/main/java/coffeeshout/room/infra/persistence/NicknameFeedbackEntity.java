package coffeeshout.room.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nickname_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NicknameFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false)
    private boolean aiFlagged;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OperatorDecision operatorDecision;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    public NicknameFeedbackEntity(String nickname, boolean aiFlagged, BigDecimal aiConfidence,
                                  OperatorDecision operatorDecision, String reason) {
        this.nickname = nickname;
        this.aiFlagged = aiFlagged;
        this.aiConfidence = aiConfidence;
        this.operatorDecision = operatorDecision;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public enum OperatorDecision {
        ALLOWED, BLOCKED
    }
}
