package coffeeshout.room.ui.response;

import coffeeshout.room.application.service.RoomCreateResult;

public record RoomCreateResponse(String joinCode, String roomSessionToken) {

    public static RoomCreateResponse of(RoomCreateResult result) {
        return new RoomCreateResponse(result.room().getJoinCode().getValue(), result.roomSessionToken());
    }
}
