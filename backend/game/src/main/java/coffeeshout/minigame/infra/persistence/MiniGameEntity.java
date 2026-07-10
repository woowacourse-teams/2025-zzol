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

    // room_session(:room)을 JPA 연관이 아니라 Long FK 컬럼으로 참조한다(ADR-0034) — DB FK 제약은 유지.
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
