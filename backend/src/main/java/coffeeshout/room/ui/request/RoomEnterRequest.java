package coffeeshout.room.ui.request;

import jakarta.validation.constraints.NotBlank;

public record RoomEnterRequest(
        @NotBlank String playerName
) {

}
