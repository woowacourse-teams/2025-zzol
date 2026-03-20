package coffeeshout.room.infra.persistence;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private LocalDateTime createdAt;

    public PlayerEntity(RoomEntity roomSession, String playerName, PlayerType playerType) {
        this.roomSession = roomSession;
        this.playerName = playerName;
        this.playerType = playerType;
        this.createdAt = LocalDateTime.now();
    }
}
