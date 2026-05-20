package coffeeshout.minigame.application;

import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.Playable;
import coffeeshout.minigame.domain.PlayableFactory;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final PlayableFactory playableFactory;
    private final ApplicationEventPublisher eventPublisher;

    public List<String> updateGames(MiniGameSelectEvent event) {
        log.info("JoinCode[{}] 게임 세션 업데이트 - 호스트: {}, 게임 목록: {}",
                event.joinCode(), event.hostName(), event.miniGameTypeNames());

        final JoinCode joinCode = new JoinCode(event.joinCode());
        final PlayerName hostName = new PlayerName(event.hostName());

        final GameSession session = getOrCreateSession(joinCode, hostName);

        final List<Playable> games = event.miniGameTypeNames().stream()
                .map(typeName -> playableFactory.create(MiniGameType.valueOf(typeName), event.joinCode()))
                .toList();
        session.replaceGames(hostName, games);

        gameSessionRepository.save(session);

        eventPublisher.publishEvent(event);

        return session.getSelectedTypes().stream()
                .map(MiniGameType::name)
                .toList();
    }

    public GameSession getOrCreateSession(JoinCode joinCode, PlayerName hostName) {
        if (gameSessionRepository.existsByJoinCode(joinCode)) {
            return gameSessionRepository.getByJoinCode(joinCode);
        }
        return new GameSession(joinCode, hostName);
    }

    public GameSession getSession(JoinCode joinCode) {
        return gameSessionRepository.getByJoinCode(joinCode);
    }

    public Optional<GameSession> findSession(JoinCode joinCode) {
        if (!gameSessionRepository.existsByJoinCode(joinCode)) {
            return Optional.empty();
        }
        return Optional.of(gameSessionRepository.getByJoinCode(joinCode));
    }

    public void deleteSession(JoinCode joinCode) {
        if (gameSessionRepository.existsByJoinCode(joinCode)) {
            gameSessionRepository.deleteByJoinCode(joinCode);
        }
    }

    public Map<PlayerName, MiniGameScore> getScores(JoinCode joinCode, MiniGameType miniGameType) {
        final GameSession session = gameSessionRepository.getByJoinCode(joinCode);
        return session.findCompletedGame(miniGameType).getScores();
    }

    public MiniGameResult getRanks(JoinCode joinCode, MiniGameType miniGameType) {
        final GameSession session = gameSessionRepository.getByJoinCode(joinCode);
        return session.findCompletedGame(miniGameType).getResult();
    }
}
