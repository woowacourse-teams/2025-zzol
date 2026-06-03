package coffeeshout.report.infra.persistence;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

    @Column(length = 45)
    private String ip;

    @Embedded
    private Reporter author;

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    // ReportCreation을 통한 신규 생성 경로 (author·ip 포함)
    public static Report create(ReportCreation creation, Clock clock) {
        return create(creation, Instant.now(clock));
    }

    public static Report create(ReportCreation creation, Instant createdAt) {
        final Report entity = new Report();
        entity.category = creation.category();
        entity.gameType = creation.gameType();
        entity.joinCode = creation.joinCode();
        entity.content = creation.content();
        entity.status = ReportStatus.PENDING;
        entity.createdAt = createdAt;
        entity.author = creation.author();
        entity.ip = creation.ip();
        return entity;
    }

    // 기존 호환 팩토리 (픽스처·목 데이터용 — author·ip 불필요 시 사용)
    public static Report createBugReport(MiniGameType gameType, String joinCode, String content, Clock clock) {
        return create(ReportCreation.bug(gameType, joinCode, content, null, null), clock);
    }

    public static Report createBugReport(MiniGameType gameType, String joinCode, String content, Instant createdAt) {
        return create(ReportCreation.bug(gameType, joinCode, content, null, null), createdAt);
    }

    public static Report createBugReport(MiniGameType gameType, String joinCode, String content, Clock clock,
                                         Reporter author) {
        return create(ReportCreation.bug(gameType, joinCode, content, author, null), clock);
    }

    public static Report createBugReport(MiniGameType gameType, String joinCode, String content, Instant createdAt,
                                         Reporter author) {
        return create(ReportCreation.bug(gameType, joinCode, content, author, null), createdAt);
    }

    public static Report createGeneralReport(ReportCategory category, String content, Clock clock) {
        return create(ReportCreation.general(category, content, null, null), clock);
    }

    public static Report createGeneralReport(ReportCategory category, String content, Instant createdAt) {
        return create(ReportCreation.general(category, content, null, null), createdAt);
    }

    public static Report createGeneralReport(ReportCategory category, String content, Clock clock,
                                             Reporter author) {
        return create(ReportCreation.general(category, content, author, null), clock);
    }

    public static Report createGeneralReport(ReportCategory category, String content, Instant createdAt,
                                             Reporter author) {
        return create(ReportCreation.general(category, content, author, null), createdAt);
    }

    public record ReportCreation(
            ReportCategory category,
            MiniGameType gameType,
            String joinCode,
            String content,
            Reporter author,
            String ip
    ) {
        public static ReportCreation bug(MiniGameType gameType, String joinCode, String content,
                                         Reporter author, String ip) {
            return new ReportCreation(ReportCategory.BUG, gameType, joinCode, content, author, ip);
        }

        public static ReportCreation general(ReportCategory category, String content,
                                              Reporter author, String ip) {
            return new ReportCreation(category, null, null, content, author, ip);
        }
    }
}
