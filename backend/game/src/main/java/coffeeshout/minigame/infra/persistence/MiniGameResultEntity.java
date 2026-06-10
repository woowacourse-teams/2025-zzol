package coffeeshout.minigame.infra.persistence;

import coffeeshout.minigame.domain.MiniGameType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "mini_game_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiniGameResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mini_game_play_id", nullable = false)
    private MiniGameEntity miniGamePlay;

    // PlayerEntity FK를 ID 참조로 분리한다(ADR-0025 FK 영속 책임 분리) — :game이 :room.infra를 모르게 한다.
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "player_rank", nullable = false)
    private Integer rank;

    @Column
    private Long score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MiniGameType miniGameType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public MiniGameResultEntity(MiniGameEntity miniGamePlay, Long playerId, Integer rank, Long score) {
        this.miniGamePlay = miniGamePlay;
        this.playerId = playerId;
        this.rank = rank;
        this.score = score;
        this.miniGameType = miniGamePlay.getMiniGameType();
        this.createdAt = LocalDateTime.now();
    }
}
