package coffeeshout.fixture;

import coffeeshout.room.domain.player.Players;

public final class PlayersFixture {

    public static final Players 호스트꾹이_루키_엠제이_한스 = new Players("A4BX") {{
        join(PlayerFixture.호스트꾹이());
        join(PlayerFixture.게스트루키());
        join(PlayerFixture.게스트엠제이());
        join(PlayerFixture.게스트한스());
    }};

    public static final Players 루키_엠제이_한스_리스트 = new Players("A4BX") {{
        join(PlayerFixture.게스트루키());
        join(PlayerFixture.게스트엠제이());
        join(PlayerFixture.게스트한스());
    }};

    private PlayersFixture() {
    }
}
