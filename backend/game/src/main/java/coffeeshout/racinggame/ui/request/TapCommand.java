package coffeeshout.racinggame.ui.request;

import jakarta.validation.constraints.NotBlank;

public record TapCommand(
        @NotBlank(message = "플레이어 이름은 필수입니다") String playerName,
        int tapCount
) {
}
