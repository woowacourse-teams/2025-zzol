package coffeeshout.global.zzolbot.infra;

import coffeeshout.global.zzolbot.domain.ZzolBotFeedback;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "zzolbot_session",
        indexes = {
                @Index(name = "idx_zzolbot_session_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_zzolbot_session_feedback", columnList = "feedback, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZzolBotSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ZzolBotFeedback feedback;

    @Column(nullable = false, length = 100)
    private String adminUsername;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static ZzolBotSessionEntity create(String question, String answer, String adminUsername) {
        final ZzolBotSessionEntity entity = new ZzolBotSessionEntity();
        entity.question = question;
        entity.answer = answer;
        entity.adminUsername = adminUsername;
        entity.createdAt = Instant.now();
        return entity;
    }

    public void applyFeedback(ZzolBotFeedback feedback) {
        this.feedback = feedback;
    }
}
