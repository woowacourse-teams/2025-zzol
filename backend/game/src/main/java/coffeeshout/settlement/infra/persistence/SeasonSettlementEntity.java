package coffeeshout.settlement.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 시즌 정산 원장. 한 행이 "한 게임에서 한 회원에게 포인트를 지급했다"는 사실이다.
 * <p>
 * (room_session_id, mini_game_type, user_id) 유니크 제약이 멱등성의 최종 방어선이다 —
 * 컨슈머 그룹의 at-least-once 재전달이나 동시 처리 경합으로 같은 결과가 두 번 들어와도
 * 두 번째 INSERT는 제약 위반으로 실패해 이중 지급이 원천 차단된다(#1610).
 */
@Entity
@Table(
        name = "season_settlement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_season_settlement_result",
                columnNames = {"room_session_id", "mini_game_type", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonSettlementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_session_id", nullable = false)
    private Long roomSessionId;

    @Column(name = "mini_game_type", nullable = false, length = 20)
    private String miniGameType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "player_rank", nullable = false)
    private Integer rank;

    @Column(nullable = false)
    private Long score;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "season_key", nullable = false, length = 10)
    private String seasonKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public SeasonSettlementEntity(
            Long roomSessionId,
            String miniGameType,
            Long userId,
            Integer rank,
            Long score,
            Integer points,
            String seasonKey
    ) {
        this.roomSessionId = roomSessionId;
        this.miniGameType = miniGameType;
        this.userId = userId;
        this.rank = rank;
        this.score = score;
        this.points = points;
        this.seasonKey = seasonKey;
        this.createdAt = LocalDateTime.now();
    }
}
