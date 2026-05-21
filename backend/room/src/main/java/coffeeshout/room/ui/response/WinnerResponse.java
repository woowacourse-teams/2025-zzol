package coffeeshout.room.ui.response;

import coffeeshout.room.domain.player.Winner;

public record WinnerResponse(
        String userCode,
        String playerName,
        Integer colorIndex,
        Integer randomAngle
) {

    public static WinnerResponse from(Winner winner) {
        return new WinnerResponse(
                winner.userCode(),
                winner.name().value(),
                winner.colorIndex(),
                winner.randomAngle()
        );
    }
}
