package coffeeshout.room.domain.roulette;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayersFixture;
import coffeeshout.fixture.RouletteFixture;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.Players;
import coffeeshout.room.domain.player.Winner;
import org.junit.jupiter.api.Test;

class RouletteTest {

    @Test
    void 당첨자를_뽑는다() {
        // given
        Players players = PlayersFixture.호스트꾹이_루키_엠제이_한스;
        Roulette roulette = RouletteFixture.고정_끝값_반환();

        // when
        Winner actual = roulette.spin(players);

        // then
        Player expected = players.getPlayers().getLast();
        assertThat(actual.name()).isEqualTo(expected.getName());
    }
}
