package coffeeshout.fixture;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.room.domain.player.Player;
import java.util.Map;

public final class MiniGameResultFixture {

    private MiniGameResultFixture() {
    }

    public static MiniGameResult 한스_루키_꾹이_엠제이() {
        Gamer 한스 = PlayerFixture.호스트한스().toGamer();
        Gamer 루키 = PlayerFixture.게스트루키().toGamer();
        Gamer 꾹이 = PlayerFixture.게스트꾹이().toGamer();
        Gamer 엠제이 = PlayerFixture.게스트엠제이().toGamer();

        return new MiniGameResult(Map.of(한스, 1, 루키, 2, 꾹이, 3, 엠제이, 4));
    }
}
