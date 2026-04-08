package coffeeshout.report.infra.persistence;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MiniGameType gameType;

    @Column(length = 10)
    private String joinCode;

    @Column(nullable = false, length = 200)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public static Report createBugReport(MiniGameType gameType, String joinCode, String content, Clock clock) {
        return createBugReport(gameType, joinCode, content, Instant.now(clock));
    }

    public static Report createBugReport(MiniGameType gameType, String joinCode, String content, Instant createdAt) {
        final Report entity = new Report();
        entity.category = ReportCategory.BUG;
        entity.gameType = gameType;
        entity.joinCode = joinCode;
        entity.content = content;
        entity.status = ReportStatus.PENDING;
        entity.createdAt = createdAt;
        return entity;
    }

    public static Report createGeneralReport(ReportCategory category, String content, Clock clock) {
        return createGeneralReport(category, content, Instant.now(clock));
    }

    public static Report createGeneralReport(ReportCategory category, String content, Instant createdAt) {
        final Report entity = new Report();
        entity.category = category;
        entity.content = content;
        entity.status = ReportStatus.PENDING;
        entity.createdAt = createdAt;
        return entity;
    }
}
