package coffeeshout.laddergame.ui.request;

import jakarta.validation.constraints.Min;

public record LadderDrawRequest(
        @Min(0) int segmentIndex
) {
}
