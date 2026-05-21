package coffeeshout.fixture;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.Playable;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;

public final class GameSessionFixture {

    private GameSessionFixture() {
    }

    public static GameSession 게임세션_게임대기(JoinCode joinCode, Playable game, PlayerName hostName) {
        GameSession session = new GameSession(joinCode, hostName);
        session.replaceGames(hostName, List.of(game));
        return session;
    }

    public static GameSession 게임세션_게임시작됨(JoinCode joinCode, Playable game, PlayerName hostName, List<PlayerName> players) {
        GameSession session = new GameSession(joinCode, hostName);
        session.replaceGames(hostName, List.of(game));
        session.startNextGame(hostName, players.stream().map(name -> new Gamer(name, null)).toList());
        return session;
    }
}
