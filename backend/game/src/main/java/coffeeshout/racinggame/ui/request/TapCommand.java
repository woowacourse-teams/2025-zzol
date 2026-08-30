package coffeeshout.racinggame.ui.request;

import jakarta.validation.constraints.PositiveOrZero;

public record TapCommand(
        @PositiveOrZero(message = "탭 횟수는 0 이상이어야 합니다") int tapCount) {}
