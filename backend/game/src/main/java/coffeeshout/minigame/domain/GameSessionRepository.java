package coffeeshout.minigame.domain;

import coffeeshout.gamecommon.JoinCode;
import java.util.Optional;

public interface GameSessionRepository {

    Optional<GameSession> findByJoinCode(JoinCode joinCode);

    boolean existsByJoinCode(JoinCode joinCode);

    GameSession save(GameSession gameSession);

    void deleteByJoinCode(JoinCode joinCode);
}
