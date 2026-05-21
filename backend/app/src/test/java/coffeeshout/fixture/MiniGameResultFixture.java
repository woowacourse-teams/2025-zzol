package coffeeshout.fixture;

import coffeeshout.minigame.domain.MiniGameResult;
import java.util.Map;

public final class MiniGameResultFixture {

    private MiniGameResultFixture() {
    }

    public static MiniGameResult 한스_루키_꾹이_엠제이() {
        return new MiniGameResult(Map.of(
                GamerFixture.게스트한스(), 1,
                GamerFixture.게스트루키(), 2,
                GamerFixture.게스트꾹이(), 3,
                GamerFixture.게스트엠제이(), 4
        ));
    }
}
