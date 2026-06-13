package coffeeshout.minigame.infra;

import static org.springframework.util.Assert.notNull;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.GameSessionRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryGameSessionRepository implements GameSessionRepository {

    private final Map<JoinCode, GameSession> sessions;

    public MemoryGameSessionRepository() {
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<GameSession> findByJoinCode(JoinCode joinCode) {
        return Optional.ofNullable(sessions.get(joinCode));
    }

    @Override
    public boolean existsByJoinCode(JoinCode joinCode) {
        return sessions.containsKey(joinCode);
    }

    @Override
    public GameSession save(GameSession gameSession) {
        sessions.put(gameSession.getJoinCode(), gameSession);
        return gameSession;
    }

    @Override
    public void deleteByJoinCode(JoinCode joinCode) {
        notNull(joinCode, "JoinCode는 null일 수 없습니다.");

        sessions.remove(joinCode);
    }
}
