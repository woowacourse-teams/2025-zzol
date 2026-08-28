package coffeeshout.wormgame.domain.event;

import coffeeshout.wormgame.domain.WormGame;
import coffeeshout.wormgame.domain.WormGameState;

public record WormGameStateChangedEvent(String joinCode, WormGameState state) {

    public static WormGameStateChangedEvent of(WormGame game, String joinCode) {
        return new WormGameStateChangedEvent(joinCode, game.getState());
    }
}
