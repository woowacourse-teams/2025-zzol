package coffeeshout.room.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roulette_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouletteResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_session_id", nullable = false)
    private RoomEntity roomSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id", nullable = false)
    private PlayerEntity winner;

    @Column(nullable = false)
    private Integer winnerProbability;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public RouletteResultEntity(RoomEntity roomSession, PlayerEntity winner, Integer winnerProbability) {
        this.roomSession = roomSession;
        this.winner = winner;
        this.winnerProbability = winnerProbability;
        this.createdAt = LocalDateTime.now();
    }
}
