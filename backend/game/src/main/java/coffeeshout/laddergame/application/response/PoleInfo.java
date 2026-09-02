package coffeeshout.laddergame.application.response;

import org.jspecify.annotations.Nullable;

public record PoleInfo(
        int index, String playerName, @Nullable Integer colorIndex) {}
