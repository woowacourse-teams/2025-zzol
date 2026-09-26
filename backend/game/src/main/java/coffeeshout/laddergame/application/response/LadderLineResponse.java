package coffeeshout.laddergame.application.response;

import org.jspecify.annotations.Nullable;

public record LadderLineResponse(
        String playerName,
        int segmentIndex,
        int row,
        @Nullable Integer colorIndex) {}
