package coffeeshout.fixture;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

@Component
public class TestDataHelper {

    private static final double DEFAULT_ROULETTE_WEIGHT = 0.7;

    private final RoomRepository roomRepository;

    public TestDataHelper(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room 방_생성(JoinCode joinCode, PlayerName hostName) {
        Room room = new Room(joinCode, hostName, DEFAULT_ROULETTE_WEIGHT);
        return roomRepository.save(room);
    }

    public Room 진행중인_방_생성(JoinCode joinCode, PlayerName hostName) {
        Room room = new Room(joinCode, hostName, DEFAULT_ROULETTE_WEIGHT);
        ReflectionTestUtils.setField(room, "roomState", RoomState.PLAYING);
        return roomRepository.save(room);
    }

    public Room 방_생성(String joinCode, String hostName) {
        return 방_생성(new JoinCode(joinCode), new PlayerName(hostName));
    }

    public Room 진행중인_방_생성(String joinCode, String hostName) {
        return 진행중인_방_생성(new JoinCode(joinCode), new PlayerName(hostName));
    }
}
