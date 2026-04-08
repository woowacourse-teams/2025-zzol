package coffeeshout.minigame.domain.repository;

import coffeeshout.minigame.domain.MiniGameType;

public interface MiniGamePersistence {

    void saveGameStart(String joinCode, MiniGameType miniGameType);
}
