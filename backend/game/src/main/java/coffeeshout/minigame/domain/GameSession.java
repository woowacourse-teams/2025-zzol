package coffeeshout.minigame.domain;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.exception.custom.BusinessException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.Getter;

/**
 * 게임 대기열·완료 목록과 진행 상태를 소유하는 애그리거트.
 * Room과 {@link JoinCode}로 1:1 연결되며, 게임 컨테이너 책임을 Room에서 분리한다(ADR-0023).
 * 내부 컬렉션은 단일 스레드(Stream Consumer) 접근을 전제로 하며, 대기열 불변식은 메서드 가드로만 변경된다.
 */
public class GameSession {

    private static final int MAX_GAMES = 5;

    @Getter
    private final JoinCode joinCode;
    @Getter
    private final Gamer host;
    private final Queue<Playable> pendingGames;
    private final List<Playable> completedGames;
    @Getter
    private GameSessionStatus status;

    public GameSession(JoinCode joinCode, Gamer host) {
        this.joinCode = joinCode;
        this.host = host;
        this.pendingGames = new LinkedList<>();
        this.completedGames = new ArrayList<>();
        this.status = GameSessionStatus.READY;
    }

    /**
     * 선택된 게임 목록을 통째로 교체한다. {@code READY} 상태에서만 허용한다.
     */
    public void replaceGames(Gamer requester, List<Playable> games) {
        validateHost(requester);
        validateReady();
        validateNoDuplicate(games);
        validateCount(games);

        pendingGames.clear();
        pendingGames.addAll(games);
    }

    /**
     * 대기열의 다음 게임을 시작한다. {@code READY} + 대기열 존재 조건에서 {@code PLAYING}으로 전이한다.
     */
    public Playable startNextGame(Gamer requester, List<Gamer> gamers) {
        validateHost(requester);
        validateReady();
        if (pendingGames.isEmpty()) {
            throw new BusinessException(GameSessionErrorCode.NO_PENDING_GAMES, "시작할 수 있는 대기 게임이 없습니다.");
        }

        final Playable currentGame = pendingGames.poll();
        currentGame.setUp(gamers);
        completedGames.add(currentGame);
        status = GameSessionStatus.PLAYING;
        return currentGame;
    }

    /**
     * 진행 중인 게임을 종료한다. 대기열이 남아 있으면 {@code READY}, 소진됐으면 {@code DONE}으로 복귀한다.
     */
    public void finishCurrentGame() {
        status = pendingGames.isEmpty() ? GameSessionStatus.DONE : GameSessionStatus.READY;
    }

    public Playable findCompletedGame(MiniGameType type) {
        return completedGames.stream()
                .filter(game -> game.getMiniGameType() == type)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        GameSessionErrorCode.GAME_NOT_FOUND, "완료된 게임 중 해당 타입이 없습니다: " + type));
    }

    public List<MiniGameType> getSelectedTypes() {
        return pendingGames.stream()
                .map(Playable::getMiniGameType)
                .toList();
    }

    /**
     * 선택된 게임 총수(대기 + 진행 중 + 완료). 진행 중인 게임은 시작 시점에 완료 목록으로 옮겨지므로
     * 세션 동안 변하지 않으며, 확률 조정 분모({@code roundCount})로 사용된다(ADR-0023 결정 3).
     */
    public int roundCount() {
        return pendingGames.size() + completedGames.size();
    }

    private void validateHost(Gamer requester) {
        if (!host.getName().equals(requester.getName())) {
            throw new BusinessException(GameSessionErrorCode.NOT_HOST, "호스트만 게임 세션을 조작할 수 있습니다.");
        }
    }

    private void validateReady() {
        if (status != GameSessionStatus.READY) {
            throw new BusinessException(GameSessionErrorCode.GAME_IN_PROGRESS, "게임 진행 중에는 대기열을 변경할 수 없습니다.");
        }
    }

    private void validateNoDuplicate(List<Playable> games) {
        final long distinctTypes = games.stream()
                .map(Playable::getMiniGameType)
                .distinct()
                .count();
        if (distinctTypes != games.size()) {
            throw new BusinessException(GameSessionErrorCode.DUPLICATE_GAME, "동일한 게임을 중복 선택할 수 없습니다.");
        }
    }

    private void validateCount(List<Playable> games) {
        if (games.size() > MAX_GAMES) {
            throw new BusinessException(GameSessionErrorCode.TOO_MANY_GAMES,
                    "선택 가능한 게임은 최대 " + MAX_GAMES + "개입니다. 현재: " + games.size());
        }
    }
}
