package coffeeshout.minigame.domain;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.Getter;

import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.state;

@Getter
public class GameSession {

    private static final int MAX_GAME_COUNT = 5;

    private final JoinCode joinCode;
    private final Queue<Playable> pendingGames;
    private final List<Playable> completedGames;

    public GameSession(JoinCode joinCode) {
        this.joinCode = joinCode;
        this.pendingGames = new LinkedList<>();
        this.completedGames = new ArrayList<>();
    }

    public void addGame(PlayerName hostName, Playable game) {
        state(pendingGames.size() <= MAX_GAME_COUNT, "미니게임은 5개 이하여야 합니다.");
        pendingGames.add(game);
    }

    public void removeGame(PlayerName hostName, MiniGameType type) {
        isTrue(pendingGames.stream().anyMatch(g -> g.getMiniGameType() == type),
                "해당 미니게임이 존재하지 않습니다.");
        pendingGames.removeIf(g -> g.getMiniGameType() == type);
    }

    public void clearPendingGames() {
        pendingGames.clear();
    }

    public Playable startNextGame(PlayerName hostName, List<Player> players) {
        state(!pendingGames.isEmpty(), "시작할 게임이 없습니다.");
        final Playable game = pendingGames.poll();
        game.setUp(players);
        completedGames.add(game);
        return game;
    }

    public Playable findCompletedGame(MiniGameType type) {
        return completedGames.stream()
                .filter(g -> g.getMiniGameType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("완료된 미니게임이 없습니다: " + type));
    }

    public List<MiniGameType> getSelectedTypes() {
        return pendingGames.stream()
                .map(Playable::getMiniGameType)
                .toList();
    }

    public List<Playable> getPendingGamesView() {
        return List.copyOf(pendingGames);
    }

    public int getTotalGameCount() {
        return pendingGames.size() + completedGames.size();
    }

    public boolean isFirstGame() {
        return completedGames.size() == 1;
    }

    public boolean hasPendingGames() {
        return !pendingGames.isEmpty();
    }
}
