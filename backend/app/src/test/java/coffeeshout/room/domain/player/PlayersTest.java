package coffeeshout.room.domain.player;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.room.domain.roulette.Probability;
import coffeeshout.room.domain.roulette.ProbabilityCalculator;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;

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

        // when
        players.adjustProbabilities(Map.of(한스.getName(), 1, 루키.getName(), 2, 꾹이.getName(), 3, 엠제이.getName(), 4), new ProbabilityCalculator(4, 5, 0.7));

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

            // when
            players.adjustProbabilities(Map.of(한스.getName(), 1, 루키.getName(), 2, 꾹이.getName(), 2), new ProbabilityCalculator(3, 1, 0.7));

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

            // when
            players.adjustProbabilities(Map.of(한스.getName(), 1, 루키.getName(), 2, 꾹이.getName(), 2, 엠제이.getName(), 4), new ProbabilityCalculator(4, 1, 0.7));

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

            // when
            players.adjustProbabilities(Map.of(한스.getName(), 1, 루키.getName(), 2, 꾹이.getName(), 3, 엠제이.getName(), 3), new ProbabilityCalculator(4, 1, 0.7));

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

            // when
            players.adjustProbabilities(Map.of(한스.getName(), 1, 루키.getName(), 2, 꾹이.getName(), 2, 엠제이.getName(), 2), new ProbabilityCalculator(4, 1, 0.7));

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

            players.adjustProbabilities(Map.of(
                    한스.getName(), 1,
                    루키.getName(), 1,
                    꾹이.getName(), 3,
                    엠제이.getName(), 3,
                    호스트유령.getName(), 5
            ), new ProbabilityCalculator(5, 1, 0.7));

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

    @Nested
    class userId_기반_조회 {

        @Test
        void userId로_플레이어_존재_여부를_확인한다() {
            // given
            final Players players = new Players("ABCD");
            players.join(Player.createGuest(new PlayerName("한스"), 100L));

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(players.existsByUserId(100L)).isTrue();
                softly.assertThat(players.existsByUserId(999L)).isFalse();
            });
        }

        @Test
        void userId가_null이면_항상_false를_반환한다() {
            // given
            final Players players = new Players("ABCD");
            players.join(Player.createGuest(new PlayerName("한스"), null));

            // when & then
            assertThat(players.existsByUserId(null)).isFalse();
        }

        @Test
        void userId로_플레이어를_제거하면_해당_플레이어만_삭제된다() {
            // given
            final Players players = new Players("ABCD");
            players.join(Player.createGuest(new PlayerName("한스"), 100L));
            players.join(Player.createGuest(new PlayerName("꾹이"), 200L));

            // when
            players.removePlayerByUserId(100L);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(players.existsByUserId(100L)).isFalse();
                softly.assertThat(players.existsByUserId(200L)).isTrue();
                softly.assertThat(players.getPlayerCount()).isEqualTo(1);
            });
        }
    }

    @Nested
    class userId_기반_제거_반환값 {

        @Test
        void 존재하는_userId로_제거하면_true를_반환한다() {
            // given
            final Players players = new Players("ABCD");
            players.join(Player.createGuest(new PlayerName("한스"), 100L));

            // when & then
            assertThat(players.removePlayerByUserId(100L)).isTrue();
        }

        @Test
        void userId가_null이면_false를_반환한다() {
            // given
            final Players players = new Players("ABCD");
            players.join(Player.createGuest(new PlayerName("한스"), 100L));

            // when & then
            assertThat(players.removePlayerByUserId(null)).isFalse();
        }

        @Test
        void 존재하지_않는_userId로_제거하면_false를_반환한다() {
            // given
            final Players players = new Players("ABCD");
            players.join(Player.createGuest(new PlayerName("한스"), 100L));

            // when & then
            assertThat(players.removePlayerByUserId(999L)).isFalse();
        }
    }

    @Nested
    class 닉네임_중복_검사 {

        @Test
        void 동일_닉네임_다른_userId면_중복이다() {
            // given
            final Players players = new Players("ABCD");
            players.join(Player.createGuest(new PlayerName("한스"), 100L));

            // when & then — userId=200인 사람이 "한스" 닉네임 사용 시도
            assertThat(players.hasDuplicateNameExceptUserId(new PlayerName("한스"), 200L)).isTrue();
        }

        @Test
        void 동일_닉네임_같은_userId면_본인이므로_중복이_아니다() {
            // given
            final Players players = new Players("ABCD");
            players.join(Player.createGuest(new PlayerName("한스"), 100L));

            // when & then — 재입장 시 본인 닉네임 그대로 유지
            assertThat(players.hasDuplicateNameExceptUserId(new PlayerName("한스"), 100L)).isFalse();
        }

        @Test
        void 다른_닉네임이면_중복이_아니다() {
            // given
            final Players players = new Players("ABCD");
            players.join(Player.createGuest(new PlayerName("한스"), 100L));

            // when & then
            assertThat(players.hasDuplicateNameExceptUserId(new PlayerName("루키"), 200L)).isFalse();
        }
    }
}
