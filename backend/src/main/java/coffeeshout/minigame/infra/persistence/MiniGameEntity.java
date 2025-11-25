package coffeeshout.minigame.infra.persistence;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.infra.persistence.RoomEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mini_game_play")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiniGameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_session_id", nullable = false)
    private RoomEntity roomSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MiniGameType miniGameType;

    public MiniGameEntity(RoomEntity roomSession, MiniGameType miniGameType) {
        this.roomSession = roomSession;
        this.miniGameType = miniGameType;
    }
}
