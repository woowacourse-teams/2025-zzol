package coffeeshout.user.ui.request;

import jakarta.validation.constraints.NotNull;

public record UpdateStatsRequest(
        @NotNull Boolean isWinner
) {
}
