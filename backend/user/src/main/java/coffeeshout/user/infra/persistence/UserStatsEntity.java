package coffeeshout.user.infra.persistence;

import coffeeshout.user.domain.UserStats;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_stats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStatsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_user_stats_user", foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE"))
    private UserEntity user;

    @Column(nullable = false)
    private int winCount;

    @Column(nullable = false)
    private int survivalStreak;

    public UserStatsEntity(UserEntity user) {
        this.user = user;
        this.winCount = 0;
        this.survivalStreak = 0;
    }

    public void update(UserStats stats) {
        this.winCount = stats.getWinCount();
        this.survivalStreak = stats.getSurvivalStreak();
    }

    public UserStats toDomain() {
        return new UserStats(user.getId(), winCount, survivalStreak);
    }
}
