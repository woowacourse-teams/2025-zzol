package coffeeshout.room.application.port;

import coffeeshout.room.domain.RoomState;

public interface RoomStatusPort {

    void updateStatus(String joinCode, RoomState state);
}
