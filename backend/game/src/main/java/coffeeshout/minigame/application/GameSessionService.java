package coffeeshout.minigame.application;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final Map<MiniGameType, MiniGameFactory> miniGameFactoryMap;

    /**
     * 방 생성 시 세션을 사전 초기화한다. 이미 존재하면 무시한다(멱등).
     */
    public void initSession(JoinCode joinCode, Gamer host) {
        if (gameSessionRepository.existsByJoinCode(joinCode)) {
            return;
        }
        gameSessionRepository.save(new GameSession(joinCode, host));
    }

    /**
     * 세션이 반드시 존재한다고 가정하는 읽기 전용 조회.
     */
    public GameSession getSession(JoinCode joinCode) {
        return gameSessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_EXIST, "게임 세션이 존재하지 않습니다."));
    }

    /**
     * 세션 유무가 불확실할 때 사용하는 조회.
     */
    public Optional<GameSession> findSession(JoinCode joinCode) {
        return gameSessionRepository.findByJoinCode(joinCode);
    }

    /**
     * 선택된 게임 목록을 통째로 교체한다. 세션이 없으면 호스트 이름으로 지연 생성한다(Step 6의
     * {@code initSession}과 멱등). 실제 흐름에서 게임 선택({@code updateGames})은 항상
     * 시작·조회보다 먼저 일어나므로 이 지연 생성이 Step 4 단독으로 동작하게 한다(ADR-0023).
     */
    public void updateGames(MiniGameSelectEvent event) {
        final JoinCode joinCode = new JoinCode(event.joinCode());
        final Gamer host = Gamer.guest(event.hostName());
        final GameSession session = findSession(joinCode)
                .orElseGet(() -> new GameSession(joinCode, host));

        final List<Playable> games = event.miniGameTypes().stream()
                .map(type -> miniGameFactoryMap.get(type).create(joinCode.getValue()))
                .toList();

        session.replaceGames(host, games);
        gameSessionRepository.save(session);
    }

    /**
     * 다음 게임을 시작한다. {@code READY} + 대기열 조건에서 {@code PLAYING}으로 전이하고 시작 게임을 반환한다.
     */
    public Playable startGame(JoinCode joinCode, Gamer requester, List<Gamer> gamers) {
        final GameSession session = getSession(joinCode);
        final Playable started = session.startNextGame(requester, gamers);
        gameSessionRepository.save(session);
        return started;
    }

    /**
     * 진행 중인 게임을 종료하고 갱신된 라운드 수(선택 게임 총수)를 반환한다.
     */
    public int finishGame(JoinCode joinCode) {
        final GameSession session = getSession(joinCode);
        session.finishCurrentGame();
        gameSessionRepository.save(session);
        return session.roundCount();
    }

    /**
     * 방 삭제 시 세션을 정리한다.
     */
    public void deleteSession(JoinCode joinCode) {
        gameSessionRepository.deleteByJoinCode(joinCode);
    }
}
