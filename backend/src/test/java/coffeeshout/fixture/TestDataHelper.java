package coffeeshout.fixture;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

@Component
public class TestDataHelper {

    @Autowired
    private RoomRepository roomRepository;

    public Room createDummyRoom(JoinCode joinCode, PlayerName hostName) {
        Room room = new Room(joinCode, hostName);
        return roomRepository.save(room);
    }

    public Room createDummyPlayingRoom(JoinCode joinCode, PlayerName hostName) {
        Room room = new Room(joinCode, hostName);
        ReflectionTestUtils.setField(room, "roomState", RoomState.PLAYING);
        return roomRepository.save(room);
    }

    public Room createDummyRoom(String joinCode, String hostName) {
        return createDummyRoom(new JoinCode(joinCode), new PlayerName(hostName));
    }

    public Room createDummyPlayingRoom(String joinCode, String hostName) {
        return createDummyPlayingRoom(new JoinCode(joinCode), new PlayerName(hostName));
    }
}
