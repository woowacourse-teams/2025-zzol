package coffeeshout.blindtimer.ui.request;

import jakarta.validation.constraints.NotBlank;

public record StopCommand(
        @NotBlank(message = "플레이어 이름은 필수입니다") String playerName
) {
}
