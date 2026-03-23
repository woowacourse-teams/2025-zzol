package coffeeshout.bombrelay.ui.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordCommand(
        @NotBlank String playerName,
        @NotBlank @Size(max = 50) String word
) {
}
