package coffeeshout.room.infra.persistence;

import coffeeshout.room.domain.RoomState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 5)
    private String joinCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoomState roomStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;

    public RoomEntity(String joinCode) {
        this.joinCode = joinCode;
        this.roomStatus = RoomState.READY;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isDone() {
        return this.roomStatus == RoomState.DONE;
    }

    public void finish() {
        this.roomStatus = RoomState.DONE;
        this.finishedAt = LocalDateTime.now();
    }

    public void updateRoomStatus(RoomState roomStatus) {
        this.roomStatus = roomStatus;
    }
}
