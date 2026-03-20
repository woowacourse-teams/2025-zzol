package coffeeshout.room.ui.response;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;

public record RoomEnterResponse(String joinCode) {

    public static RoomEnterResponse from(Room room) {
        JoinCode joinCode = room.getJoinCode();
        return new RoomEnterResponse(joinCode.getValue());
    }
}
