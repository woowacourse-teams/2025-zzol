package coffeeshout.room.ui.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomEnterRequest(
        @NotBlank String playerName,
        @Valid @NotNull SelectedMenuRequest menu
) {

}
