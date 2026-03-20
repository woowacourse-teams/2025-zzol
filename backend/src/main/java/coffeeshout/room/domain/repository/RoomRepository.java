package coffeeshout.room.domain.repository;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import java.util.Optional;

public interface RoomRepository {

    Optional<Room> findByJoinCode(JoinCode joinCode);

    boolean existsByJoinCode(JoinCode joinCode);

    Room save(Room room);

    void deleteByJoinCode(JoinCode joinCode);
}
