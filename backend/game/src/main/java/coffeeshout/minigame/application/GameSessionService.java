package coffeeshout.minigame.application;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameSelectEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final Map<MiniGameType, MiniGameFactory> miniGameFactoryMap;

    /**
     * 팩토리 맵은 {@code :game}이 자기 팩토리 빈 전부를 보므로 composition root 없이 여기서 조립한다
     * (ADR-0025 Step 5 — 기존 {@code :app}의 {@code MiniGameFactoryConfig} 대체).
     */
    public GameSessionService(GameSessionRepository gameSessionRepository, List<MiniGameFactory> miniGameFactories) {
        this.gameSessionRepository = gameSessionRepository;
        this.miniGameFactoryMap = new EnumMap<>(MiniGameType.class);
        miniGameFactories.forEach(factory -> miniGameFactoryMap.put(factory.type(), factory));
    }

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
     * 선택된 게임 목록을 통째로 교체한다. 세션은 방 생성 시 {@code GameSessionInitConsumer}가
     * 권위 있는 호스트({@code GameRoomCreatedEvent}의 hostName)로 사전 생성하므로 여기서는 반드시 존재한다고
     * 가정한다(지연 생성 폴백 없음 — ADR-0025 결정 4/Option B). 호스트 검증을 GameSession이 단독 수행하려면
     * 세션 host가 select 이벤트의 주장값이 아니라 방 생성 시점의 권위값이어야 하기 때문이다. init보다 select가
     * 먼저 도달하는 극히 짧은 창에서는 {@code getSession}이 예외를 던져 {@code EventDispatcher}가 격리·스킵하고,
     * 클라이언트의 재전송으로 복구된다(거짓 호스트를 신뢰하는 대신 일시 거부).
     */
    public void updateGames(MiniGameSelectEvent event) {
        final JoinCode joinCode = new JoinCode(event.joinCode());
        final GameSession session = getSession(joinCode);

        final List<Playable> games = event.miniGameTypes().stream()
                .map(type -> miniGameFactoryMap.get(type).create(joinCode.getValue()))
                .toList();

        session.replaceGames(Gamer.guest(event.hostName()), games);
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
     * 호스트 승계 시 세션의 호스트를 갱신한다. 생성·정리와 같은 생명주기 이벤트이므로 세션이 없으면
     * 조용히 건너뛴다(인스턴스 재시작으로 인메모리 세션이 소실된 경우 등 — {@code initSession}/
     * {@code deleteSession}과 동일한 멱등·비throw 정책).
     */
    public void updateHost(JoinCode joinCode, Gamer newHost) {
        findSession(joinCode).ifPresent(session -> {
            session.updateHost(newHost);
            gameSessionRepository.save(session);
        });
    }

    /**
     * 방 삭제 시 세션을 정리한다.
     */
    public void deleteSession(JoinCode joinCode) {
        gameSessionRepository.deleteByJoinCode(joinCode);
    }

    /**
     * 시작된 게임(진행 중 포함)의 점수를 조회한다.
     */
    public Map<Gamer, MiniGameScore> getScores(JoinCode joinCode, MiniGameType miniGameType) {
        return getSession(joinCode).findCompletedGame(miniGameType).getScores();
    }

    /**
     * 시작된 게임(진행 중 포함)의 순위를 조회한다.
     */
    public MiniGameResult getRanks(JoinCode joinCode, MiniGameType miniGameType) {
        return getSession(joinCode).findCompletedGame(miniGameType).getResult();
    }

    /**
     * 선택된(대기 중인) 게임 타입 목록을 조회한다. 세션이 아직 없으면 빈 목록을 반환한다
     * (게임 선택 전의 방 — 기존 Room 대기열 조회와 동일한 의미).
     */
    public List<MiniGameType> getSelectedTypes(JoinCode joinCode) {
        return findSession(joinCode)
                .map(GameSession::getSelectedTypes)
                .orElseGet(List::of);
    }
}
