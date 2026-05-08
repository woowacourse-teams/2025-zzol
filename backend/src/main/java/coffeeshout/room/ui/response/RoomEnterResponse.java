package coffeeshout.room.ui.response;

import coffeeshout.room.domain.Room;

public record RoomEnterResponse(String joinCode, String roomSessionToken) {

    public static RoomEnterResponse of(Room room, String roomSessionToken) {
        return new RoomEnterResponse(room.getJoinCode().getValue(), roomSessionToken);
    }
}
