package coffeeshout.minigame.infra.persistence;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.infra.persistence.PlayerEntity;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Column(name = "player_rank", nullable = false)
    private Integer rank;

    @Column
    private Long score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MiniGameType miniGameType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public MiniGameResultEntity(MiniGameEntity miniGamePlay, PlayerEntity player, Integer rank, Long score) {
        this.miniGamePlay = miniGamePlay;
        this.player = player;
        this.rank = rank;
        this.score = score;
        this.miniGameType = miniGamePlay.getMiniGameType();
        this.createdAt = LocalDateTime.now();
    }
}
