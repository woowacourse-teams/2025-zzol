package coffeeshout.bombrelay.ui.request;

import jakarta.validation.constraints.NotBlank;

public record WordCommand(
        @NotBlank String playerName,
        @NotBlank String word
) {
}
