package coffeeshout.minigame.domain;

import coffeeshout.room.domain.Playable;

public interface PlayableFactory {

    Playable create(MiniGameType type, String joinCode);
}
