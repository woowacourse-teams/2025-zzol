package coffeeshout.fixture;

import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.Playable;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;

public final class GameSessionFixture {

    private GameSessionFixture() {
    }

    public static GameSession 게임세션_게임대기(JoinCode joinCode, Playable game, PlayerName hostName) {
        GameSession session = new GameSession(joinCode);
        session.addGame(hostName, game);
        return session;
    }

    public static GameSession 게임세션_게임시작됨(JoinCode joinCode, Playable game, PlayerName hostName, List<Player> players) {
        GameSession session = new GameSession(joinCode);
        session.addGame(hostName, game);
        session.startNextGame(hostName, players);
        return session;
    }
}
