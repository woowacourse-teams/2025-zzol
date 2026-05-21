package coffeeshout.dashboard.domain;

import coffeeshout.minigame.domain.MiniGameType;

public record GamePlayCountResponse(
        MiniGameType gameType,
        Long playCount
) {
}
