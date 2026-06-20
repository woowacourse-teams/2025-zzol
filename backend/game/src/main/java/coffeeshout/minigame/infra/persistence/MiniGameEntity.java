package coffeeshout.minigame.infra.persistence;

import coffeeshout.minigame.domain.MiniGameType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    // RoomEntity FK를 ID 참조로 분리한다(ADR-0025 FK 영속 책임 분리) — :game이 :room.infra를 모르게 한다.
    @Column(name = "room_session_id", nullable = false)
    private Long roomSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MiniGameType miniGameType;

    public MiniGameEntity(Long roomSessionId, MiniGameType miniGameType) {
        this.roomSessionId = roomSessionId;
        this.miniGameType = miniGameType;
    }
}
