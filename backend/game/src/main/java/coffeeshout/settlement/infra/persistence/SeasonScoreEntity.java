package coffeeshout.settlement.infra.persistence;

import coffeeshout.settlement.domain.SeasonTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원의 시즌 누적 성적. 원장(season_settlement)의 합계를 시즌·회원 단위로 유지한다.
 * 리더보드 ZSET은 이 테이블의 total_points를 진실 공급원으로 삼아 절대값으로 갱신한다(#1610).
 */
@Entity
@Table(
        name = "season_score",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_season_score_user",
                columnNames = {"season_key", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_key", nullable = false, length = 10)
    private String seasonKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_points", nullable = false)
    private Long totalPoints;

    @Column(name = "games_played", nullable = false)
    private Integer gamesPlayed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeasonTier tier;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public SeasonScoreEntity(String seasonKey, Long userId, long initialPoints) {
        this.seasonKey = seasonKey;
        this.userId = userId;
        this.totalPoints = initialPoints;
        this.gamesPlayed = 1;
        this.tier = SeasonTier.fromPoints(initialPoints);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTier(SeasonTier tier) {
        this.tier = tier;
        this.updatedAt = LocalDateTime.now();
    }
}
