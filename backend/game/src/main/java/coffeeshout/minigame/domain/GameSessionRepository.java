package coffeeshout.minigame.domain;

import coffeeshout.room.domain.JoinCode;

public interface GameSessionRepository {

    GameSession save(GameSession gameSession);

    GameSession getByJoinCode(JoinCode joinCode);

    boolean existsByJoinCode(JoinCode joinCode);

    void deleteByJoinCode(JoinCode joinCode);
}
