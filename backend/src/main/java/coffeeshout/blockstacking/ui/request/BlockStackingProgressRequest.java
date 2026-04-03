package coffeeshout.blockstacking.ui.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

public record BlockStackingProgressRequest(
        @Positive(message = "층수는 1 이상이어야 합니다") int floor,
        double movingBlockX,
        double stackTopX,
        @Positive(message = "스택 너비는 0보다 커야 합니다") double stackTopWidth
) {

    @AssertTrue(message = "좌표 값은 유한한 숫자여야 합니다")
    boolean isFiniteCoordinates() {
        return Double.isFinite(movingBlockX) && Double.isFinite(stackTopX) && Double.isFinite(stackTopWidth);
    }
}
