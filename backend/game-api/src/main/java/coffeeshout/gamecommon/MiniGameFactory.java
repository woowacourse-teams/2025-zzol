package coffeeshout.gamecommon;

import coffeeshout.minigame.domain.MiniGameType;

public interface MiniGameFactory {

    MiniGameType type();

    Playable create(String joinCode);
}
