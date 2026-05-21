package coffeeshout.minigame.domain;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class GameSession {

    private final JoinCode joinCode;
    private final PlayerName hostName;

    @Getter(AccessLevel.NONE)
    private final Queue<Playable> pendingGames;

    @Getter(AccessLevel.NONE)
    private final List<Playable> completedGames;

    public GameSession(JoinCode joinCode, PlayerName hostName) {
        this.joinCode = joinCode;
        this.hostName = hostName;
        this.pendingGames = new LinkedList<>();
        this.completedGames = new ArrayList<>();
    }

    public synchronized void replaceGames(PlayerName hostName, List<Playable> games) {
        validateHost(hostName);
        final long distinctCount = games.stream()
                .map(Playable::getMiniGameType)
                .distinct()
                .count();
        if (distinctCount != games.size()) {
            throw new BusinessException(GameSessionErrorCode.DUPLICATE_GAME, "동일한 게임을 중복으로 선택할 수 없습니다.");
        }
        pendingGames.clear();
        pendingGames.addAll(games);
    }

    public synchronized Playable startNextGame(PlayerName hostName, List<Gamer> gamers) {
        validateHost(hostName);
        if (pendingGames.isEmpty()) {
            throw new BusinessException(GameSessionErrorCode.NO_PENDING_GAMES, "시작할 게임이 없습니다.");
        }
        final Playable game = pendingGames.poll();
        game.setUp(gamers);
        completedGames.add(game);
        return game;
    }

    public synchronized Playable findCompletedGame(MiniGameType type) {
        return completedGames.stream()
                .filter(g -> g.getMiniGameType() == type)
                .findFirst()
                .orElseThrow(() -> new BusinessException(GameSessionErrorCode.GAME_NOT_FOUND, "완료된 미니게임이 없습니다: " + type));
    }

    public synchronized List<MiniGameType> getSelectedTypes() {
        return pendingGames.stream()
                .map(Playable::getMiniGameType)
                .toList();
    }

    public synchronized List<Playable> getPendingGamesView() {
        return List.copyOf(pendingGames);
    }

    public synchronized List<Playable> getCompletedGames() {
        return List.copyOf(completedGames);
    }

    public synchronized int getTotalGameCount() {
        return pendingGames.size() + completedGames.size();
    }

    public synchronized boolean isFirstGame() {
        return completedGames.size() == 1;
    }

    public synchronized boolean hasPendingGames() {
        return !pendingGames.isEmpty();
    }

    private void validateHost(PlayerName hostName) {
        if (!this.hostName.equals(hostName)) {
            throw new BusinessException(GameSessionErrorCode.NOT_HOST, "호스트만 게임 세션을 관리할 수 있습니다.");
        }
    }
}
