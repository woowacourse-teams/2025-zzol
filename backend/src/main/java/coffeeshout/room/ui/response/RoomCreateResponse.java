package coffeeshout.room.ui.response;

import coffeeshout.room.domain.Room;

public record RoomCreateResponse(String joinCode, String roomSessionToken) {

    public static RoomCreateResponse of(Room room, String roomSessionToken) {
        return new RoomCreateResponse(room.getJoinCode().getValue(), roomSessionToken);
    }
}
