package coffeeshout.numberpoker.domain;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PokerRoundTest {

    Player 꾹이 = PlayerFixture.호스트꾹이();
    Player 루키 = PlayerFixture.게스트루키();
    Player 엠제이 = PlayerFixture.게스트엠제이();

    // 꾹이: [9,9] 페어→WIN 확률 높음, 루키: [7,3] 하이카드, 엠제이: [2,1] 하이카드
    // 딜러: [5,4] 하이카드
    PokerRound round;
    Dealer dealer;

    @BeforeEach
    void setUp() {
        dealer = new Dealer(new PokerCard(5), new PokerCard(4));
        final Map<Player, PlayerPokerHand> hands = Map.of(
                꾹이, new PlayerPokerHand(꾹이, new PokerCard(9), new PokerCard(9)),
                루키, new PlayerPokerHand(루키, new PokerCard(7), new PokerCard(3)),
                엠제이, new PlayerPokerHand(엠제이, new PokerCard(2), new PokerCard(1))
        );
        round = new PokerRound(1, 3, dealer, hands);
    }

    @Nested
    class 라운드_번호 {

        @Test
        void 첫_번째_라운드를_확인한다() {
            assertThat(round.isFirst()).isTrue();
        }

        @Test
        void 마지막_라운드를_확인한다() {
            final PokerRound lastRound = new PokerRound(3, 3, dealer, Map.of());

            assertThat(lastRound.isLast()).isTrue();
        }
    }

    @Nested
    class 폴드_처리 {

        @Test
        void 플레이어가_폴드하면_해당_플레이어가_FOLDED_상태가_된다() {
            round.fold(꾹이, PokerPhase.STAGE_1);

            assertThat(round.isPlayerFolded(꾹이)).isTrue();
        }

        @Test
        void 폴드하지_않은_플레이어는_ACTIVE_상태다() {
            assertThat(round.isPlayerFolded(꾹이)).isFalse();
        }

        @Test
        void 존재하지_않는_플레이어가_폴드하면_예외가_발생한다() {
            final Player 유령 = PlayerFixture.호스트유령();

            assertCoffeeShoutException(
                    () -> round.fold(유령, PokerPhase.STAGE_1),
                    NumberPokerErrorCode.PLAYER_NOT_FOUND
            );
        }
    }

    @Nested
    class 전원_폴드_확인 {

        @Test
        void 전원_폴드_전에는_false다() {
            round.fold(꾹이, PokerPhase.STAGE_1);
            round.fold(루키, PokerPhase.STAGE_1);

            assertThat(round.isAllFolded()).isFalse();
        }

        @Test
        void 전원_폴드하면_true다() {
            round.fold(꾹이, PokerPhase.STAGE_1);
            round.fold(루키, PokerPhase.STAGE_1);
            round.fold(엠제이, PokerPhase.STAGE_1);

            assertThat(round.isAllFolded()).isTrue();
        }
    }

    @Nested
    class 결과_계산 {

        @Test
        void SHOWDOWN_후_각_플레이어_결과를_계산한다() {
            // 딜러: [5,4] 하이카드 5
            // 꾹이: [9,9] 페어 → WIN
            // 루키: [7,3] 하이카드 7 → WIN
            // 엠제이: [2,1] 하이카드 2 → LOSE
            dealer.revealAll();

            final Map<Player, PokerRoundResult> results = round.calculateResults();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(results.get(꾹이)).isEqualTo(PokerRoundResult.WIN);
                softly.assertThat(results.get(루키)).isEqualTo(PokerRoundResult.WIN);
                softly.assertThat(results.get(엠제이)).isEqualTo(PokerRoundResult.LOSE);
            });
        }

        @Test
        void 폴드한_플레이어는_폴드_결과를_가진다() {
            round.fold(루키, PokerPhase.STAGE_1);
            round.fold(엠제이, PokerPhase.STAGE_2);
            dealer.revealAll();

            final Map<Player, PokerRoundResult> results = round.calculateResults();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(results.get(루키)).isEqualTo(PokerRoundResult.STAGE_1_FOLD);
                softly.assertThat(results.get(엠제이)).isEqualTo(PokerRoundResult.STAGE_2_FOLD);
            });
        }
    }

    @Nested
    class 준비_완료_처리 {

        @Test
        void 전원_레디_전에는_false다() {
            round.markReady(꾹이);
            round.markReady(루키);

            assertThat(round.isAllReady(List.of(꾹이, 루키, 엠제이))).isFalse();
        }

        @Test
        void 전원_레디하면_true다() {
            round.markReady(꾹이);
            round.markReady(루키);
            round.markReady(엠제이);

            assertThat(round.isAllReady(List.of(꾹이, 루키, 엠제이))).isTrue();
        }

        @Test
        void 중복_레디는_무시된다() {
            round.markReady(꾹이);
            round.markReady(꾹이);
            round.markReady(루키);
            round.markReady(엠제이);

            assertThat(round.isAllReady(List.of(꾹이, 루키, 엠제이))).isTrue();
        }
    }
}
