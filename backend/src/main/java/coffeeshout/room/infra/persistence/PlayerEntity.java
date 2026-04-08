package coffeeshout.room.infra.persistence;

import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.PlayerType;
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
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player",
       indexes = @Index(name = "idx_player_player_name", columnList = "player_name"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_session_id", nullable = false)
    private RoomEntity roomSession;

    @Column(nullable = false, length = 10)
    private String playerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PlayerType playerType;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    public PlayerEntity(RoomEntity roomSession, String playerName, PlayerType playerType) {
        this.roomSession = roomSession;
        this.playerName = playerName;
        this.playerType = playerType;
        this.createdAt = LocalDateTime.now();
    }

    // MapStruct @AfterMapping에서 createdAt 초기화에 사용
    public void initCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }

    public void updatePlayerName(PlayerName playerName) {
        this.playerName = playerName.value();
    }
}
