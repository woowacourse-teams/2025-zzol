package coffeeshout.blockstacking.ui.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record BlockStackingProgressRequest(
        @NotBlank(message = "플레이어 이름은 필수입니다") String playerName,
        @Positive(message = "층수는 1 이상이어야 합니다") int floor,
        double movingBlockX,
        double stackTopX,
        double stackTopWidth
) {
}
