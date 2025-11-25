package coffeeshout.room.domain.repository;

import static org.springframework.util.Assert.notNull;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryRoomRepository implements RoomRepository {

    private final Map<JoinCode, Room> rooms;

    public MemoryRoomRepository() {
        this.rooms = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<Room> findByJoinCode(JoinCode joinCode) {
        return Optional.ofNullable(rooms.get(joinCode));
    }

    @Override
    public boolean existsByJoinCode(JoinCode joinCode) {
        return rooms.containsKey(joinCode);
    }

    @Override
    public Room save(Room room) {
        rooms.put(room.getJoinCode(), room);
        return rooms.get(room.getJoinCode());
    }

    @Override
    public void deleteByJoinCode(JoinCode joinCode) {
        notNull(joinCode, "JoinCode는 null일 수 없습니다.");

        rooms.remove(joinCode);
    }
}
