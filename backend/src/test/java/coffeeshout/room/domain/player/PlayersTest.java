package coffeeshout.room.domain.player;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.room.domain.roulette.Probability;
import coffeeshout.room.domain.roulette.ProbabilityCalculator;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayersTest {

    @Test
    void 순위를_기반으로_확률을_조정한다() {
        // given
        final Players players = new Players("ABC23");
        Player 한스 = PlayerFixture.호스트한스();
        Player 루키 = PlayerFixture.게스트루키();
        Player 꾹이 = PlayerFixture.게스트꾹이();
        Player 엠제이 = PlayerFixture.게스트엠제이();

        players.join(한스);
        players.join(루키);
        players.join(꾹이);
        players.join(엠제이);

        MiniGameResult miniGameResult = new MiniGameResult(Map.of(한스, 1, 루키, 2, 꾹이, 3, 엠제이, 4));

        // when
        players.adjustProbabilities(miniGameResult, new ProbabilityCalculator(4, 5));

        // then
        assertThat(players.getPlayer(PlayerFixture.호스트한스().getName()).getProbability())
                .isEqualTo(new Probability((int) (2500 - 500 * 0.7)));
        assertThat(players.getPlayer(PlayerFixture.게스트루키().getName()).getProbability())
                .isEqualTo(new Probability((int) (2500 - 250 * 0.7)));
        assertThat(players.getPlayer(PlayerFixture.게스트꾹이().getName()).getProbability())
                .isEqualTo(new Probability((int) (2500 + 250 * 0.7)));
        assertThat(players.getPlayer(PlayerFixture.게스트엠제이().getName()).getProbability())
                .isEqualTo(new Probability((int) (2500 + 500 * 0.7)));
    }

    @Nested
    class 동점자_테스트 {

        @Test
        void _3명_중_2등_동점자_2명() {
            // given
            final Players players = new Players("ABC23");
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();
            Player 꾹이 = PlayerFixture.게스트꾹이();

            players.join(한스);
            players.join(루키);
            players.join(꾹이);

            MiniGameResult miniGameResult = new MiniGameResult(Map.of(한스, 1, 루키, 2, 꾹이, 2));

            // when
            players.adjustProbabilities(miniGameResult, new ProbabilityCalculator(3, 1));

            // then
            // 한스 1등, 루키/꾹이 2등 동점 (2명이므로 확률 조정량을 2로 나눔)
            // 기본 확률: 3333
            // 1등 조정: -3333 * 0.7 = -2333
            // 2등 조정: +3333 * 0.7 / 2 = +1166 (동점자 2명이므로 나눔)

            assertThat(players.getPlayer(PlayerFixture.호스트한스().getName()).getProbability())
                    .isEqualTo(new Probability((3333 - (int) (3333 * 0.7))));
            assertThat(players.getPlayer(PlayerFixture.게스트루키().getName()).getProbability())
                    .isEqualTo(new Probability((3333 + (int) (3333 * 0.7) / 2)));
            assertThat(players.getPlayer(PlayerFixture.게스트꾹이().getName()).getProbability())
                    .isEqualTo(new Probability((3333 + (int) (3333 * 0.7) / 2)));
        }

        @Test
        void _4명_중_2등_동점자_2명() {
            // given
            final Players players = new Players("ABC23");
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();
            Player 꾹이 = PlayerFixture.게스트꾹이();
            Player 엠제이 = PlayerFixture.게스트엠제이();

            players.join(한스);
            players.join(루키);
            players.join(꾹이);
            players.join(엠제이);

            MiniGameResult miniGameResult = new MiniGameResult(Map.of(한스, 1, 루키, 2, 꾹이, 2, 엠제이, 4));

            // when
            players.adjustProbabilities(miniGameResult, new ProbabilityCalculator(4, 1));

            // then
            // 한스 1등, 루키/꾹이 2등 동점 (2명이므로 확률 조정량을 2로 나눔)
            // 기본 확률: 2500
            // 1등 조정: -2500 * 0.7 = -1750
            // 2등 조정: 조정 x
            // 4등 조정 +2500 * 0.7 / 2 = +1750 (동점자 2명이므로 나눔)

            SoftAssertions.assertSoftly(
                    softly -> {
                        softly.assertThat(players.getPlayer(PlayerFixture.호스트한스().getName()).getProbability())
                                .isEqualTo(new Probability((2500 - (int) (2500 * 0.7))));
                        softly.assertThat(players.getPlayer(PlayerFixture.게스트루키().getName()).getProbability())
                                .isEqualTo(new Probability(2500));
                        softly.assertThat(players.getPlayer(PlayerFixture.게스트꾹이().getName()).getProbability())
                                .isEqualTo(new Probability(2500));
                        softly.assertThat(players.getPlayer(PlayerFixture.게스트엠제이().getName()).getProbability())
                                .isEqualTo(new Probability((2500 + (int) (2500 * 0.7))));
                    }
            );
        }

        @Test
        void _4명_중_3등_동점자_2명() {
            // given
            final Players players = new Players("ABC23");
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();
            Player 꾹이 = PlayerFixture.게스트꾹이();
            Player 엠제이 = PlayerFixture.게스트엠제이();

            players.join(한스);
            players.join(꾹이);
            players.join(엠제이);
            players.join(루키);

            MiniGameResult miniGameResult = new MiniGameResult(Map.of(한스, 1, 루키, 2, 꾹이, 3, 엠제이, 3));

            // when
            players.adjustProbabilities(miniGameResult, new ProbabilityCalculator(4, 1));

            SoftAssertions.assertSoftly(
                    softly -> {
                        softly.assertThat(players.getPlayer(PlayerFixture.호스트한스().getName()).getProbability())
                                .isEqualTo(new Probability((2500 - (int) (2500 * 0.7))));
                        softly.assertThat(players.getPlayer(PlayerFixture.게스트루키().getName()).getProbability())
                                .isEqualTo(new Probability(2500 - (int) (1250 * 0.7)));
                        softly.assertThat(players.getPlayer(PlayerFixture.게스트꾹이().getName()).getProbability())
                                .isEqualTo(new Probability(2500 + (int) (3750 * 0.7) / 2));
                        softly.assertThat(players.getPlayer(PlayerFixture.게스트엠제이().getName()).getProbability())
                                .isEqualTo(new Probability(2500 + (int) (3750 * 0.7) / 2));
                    }
            );
        }

        @Test
        void _4명_중_2등_동점자_3명() {
            // given
            Players players = new Players("ABC23");
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();
            Player 꾹이 = PlayerFixture.게스트꾹이();
            Player 엠제이 = PlayerFixture.게스트엠제이();

            players.join(한스);
            players.join(루키);
            players.join(꾹이);
            players.join(엠제이);

            MiniGameResult miniGameResult = new MiniGameResult(Map.of(한스, 1, 루키, 2, 꾹이, 2, 엠제이, 2));

            // when
            players.adjustProbabilities(miniGameResult, new ProbabilityCalculator(4, 1));

            // then
            SoftAssertions.assertSoftly(
                    softly -> {
                        softly.assertThat(players.getPlayer(PlayerFixture.호스트한스().getName()).getProbability())
                                .isEqualTo(new Probability((2500 - (int) (2500 * 0.7))));
                        softly.assertThat(players.getPlayer(PlayerFixture.게스트루키().getName()).getProbability())
                                .isEqualTo(new Probability(2500 + (int) (2500 * 0.7) / 3));
                        softly.assertThat(players.getPlayer(PlayerFixture.게스트꾹이().getName()).getProbability())
                                .isEqualTo(new Probability(2500 + (int) (2500 * 0.7) / 3));
                        softly.assertThat(players.getPlayer(PlayerFixture.게스트엠제이().getName()).getProbability())
                                .isEqualTo(new Probability(2500 + (int) (2500 * 0.7) / 3));
                    }
            );
        }

        @Test
        void _5명_중_1등_동점자_2명_3등_동점자_2명() {
            // given
            Player 한스 = PlayerFixture.게스트한스();
            Player 루키 = PlayerFixture.게스트루키();
            Player 꾹이 = PlayerFixture.게스트꾹이();
            Player 엠제이 = PlayerFixture.게스트엠제이();
            Player 호스트유령 = PlayerFixture.호스트유령();

            Players players = new Players("ABC23");

            players.join(한스);
            players.join(루키);
            players.join(꾹이);
            players.join(엠제이);
            players.join(호스트유령);

            MiniGameResult miniGameResult = new MiniGameResult(Map.of(
                    한스, 1,
                    루키, 1,
                    꾹이, 3,
                    엠제이, 3,
                    호스트유령, 5
            ));

            players.adjustProbabilities(miniGameResult, new ProbabilityCalculator(5, 1));

            // then
            SoftAssertions.assertSoftly(
                    softly -> {
                        softly.assertThat(players.getPlayer(new PlayerName("한스")).getProbability())
                                .isEqualTo(new Probability((2000 - (2100 / 2))));
                        softly.assertThat(players.getPlayer(new PlayerName("루키")).getProbability())
                                .isEqualTo(new Probability(2000 - (2100 / 2)));
                        softly.assertThat(players.getPlayer(new PlayerName("꾹이")).getProbability())
                                .isEqualTo(new Probability(2000 + 700 / 2));
                        softly.assertThat(players.getPlayer(new PlayerName("엠제이")).getProbability())
                                .isEqualTo(new Probability(2000 + 700 / 2));
                        softly.assertThat(players.getPlayer(new PlayerName("유령")).getProbability())
                                .isEqualTo(new Probability(2000 + 1400));
                    }
            );
        }
    }
}
