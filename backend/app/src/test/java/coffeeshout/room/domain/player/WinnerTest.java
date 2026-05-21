package coffeeshout.room.domain.player;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.room.domain.roulette.Probability;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WinnerTest {

    @Nested
    class Winner_생성 {

        @Test
        void 회원_플레이어의_userCode가_Winner에_포함된다() {
            final Player player = Player.createGuest(new PlayerName("꾹이"), 1L, "ABCD1");
            player.assignColorIndex(2);
            player.updateProbability(new Probability(3000));

            final Winner winner = Winner.from(player);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(winner.name().value()).isEqualTo("꾹이");
                softly.assertThat(winner.colorIndex()).isEqualTo(2);
                softly.assertThat(winner.userCode()).isEqualTo("ABCD1");
                softly.assertThat(winner.randomAngle()).isBetween(0, 100);
            });
        }

        @Test
        void 비로그인_플레이어의_userCode는_null이다() {
            final Player player = Player.createGuest(new PlayerName("익명"));
            player.assignColorIndex(0);
            player.updateProbability(new Probability(5000));

            final Winner winner = Winner.from(player);

            assertThat(winner.userCode()).isNull();
        }
    }
}
