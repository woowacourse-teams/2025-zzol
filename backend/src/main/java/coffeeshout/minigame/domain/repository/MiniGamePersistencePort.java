package coffeeshout.minigame.domain.repository;

import coffeeshout.minigame.domain.MiniGameType;

public interface MiniGamePersistencePort {

    void saveGameStart(String joinCode, MiniGameType miniGameType);
}
