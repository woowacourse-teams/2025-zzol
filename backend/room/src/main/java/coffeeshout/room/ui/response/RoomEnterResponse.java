package coffeeshout.room.ui.response;

import coffeeshout.room.application.service.RoomEnterResult;

public record RoomEnterResponse(String joinCode, String roomSessionToken) {

    public static RoomEnterResponse of(RoomEnterResult result) {
        return new RoomEnterResponse(result.room().getJoinCode().getValue(), result.roomSessionToken());
    }
}
