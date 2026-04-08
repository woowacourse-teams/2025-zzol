package coffeeshout.room.domain.repository;

import coffeeshout.room.domain.player.Winner;

public interface RouletteResultPersistence {

    void updateRoomStatusToRoulette(String joinCode);

    void finishRoomAndSaveResult(String joinCode, Winner winner);
}
