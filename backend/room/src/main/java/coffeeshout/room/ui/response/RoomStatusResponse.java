package coffeeshout.room.ui.response;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.RoomState;

public record RoomStatusResponse(
        String joinCode,
        RoomState roomState
) {

    public static RoomStatusResponse of(JoinCode joinCode, RoomState roomState) {
        return new RoomStatusResponse(
                joinCode.getValue(),
                roomState
        );
    }
}
