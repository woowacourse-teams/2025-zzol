package coffeeshout.minigame.infra;

import coffeeshout.exception.GlobalErrorCode;
import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.room.domain.JoinCode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryGameSessionRepository implements GameSessionRepository {

    private final Map<String, GameSession> store = new ConcurrentHashMap<>();

    @Override
    public GameSession save(GameSession gameSession) {
        store.put(gameSession.getJoinCode().getValue(), gameSession);
        return gameSession;
    }

    @Override
    public GameSession getByJoinCode(JoinCode joinCode) {
        final GameSession session = store.get(joinCode.getValue());
        if (session == null) {
            throw new BusinessException(
                    GlobalErrorCode.NOT_EXIST,
                    "게임 세션이 존재하지 않습니다: " + joinCode.getValue()
            );
        }
        return session;
    }

    @Override
    public boolean existsByJoinCode(JoinCode joinCode) {
        return store.containsKey(joinCode.getValue());
    }

    @Override
    public void deleteByJoinCode(JoinCode joinCode) {
        store.remove(joinCode.getValue());
    }
}
