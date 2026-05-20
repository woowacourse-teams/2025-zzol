package coffeeshout.fixture;

import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Map;

public final class MiniGameResultFixture {

    private MiniGameResultFixture() {
    }

    public static MiniGameResult 한스_루키_꾹이_엠제이() {
        return new MiniGameResult(Map.of(
                new PlayerName("한스"), 1,
                new PlayerName("루키"), 2,
                new PlayerName("꾹이"), 3,
                new PlayerName("엠제이"), 4
        ));
    }
}


