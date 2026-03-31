package coffeeshout.numberpoker.domain;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.room.domain.player.Player;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerPokerHandTest {

    Player 꾹이 = PlayerFixture.호스트꾹이();
    PlayerPokerHand hand;

    @BeforeEach
    void setUp() {
        hand = new PlayerPokerHand(꾹이, new PokerCard(8), new PokerCard(3));
    }

    @Nested
    class 초기_상태 {

        @Test
        void 플레이어_핸드는_ACTIVE_상태로_시작한다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(hand.isActive()).isTrue();
                softly.assertThat(hand.isFolded()).isFalse();
            });
        }
    }

    @Nested
    class 폴드 {

        @Test
        void STAGE_1에서_폴드하면_STAGE_1_FOLD_결과를_가진다() {
            hand.fold(PokerPhase.STAGE_1);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(hand.isFolded()).isTrue();
                softly.assertThat(hand.isActive()).isFalse();
                softly.assertThat(hand.getFoldResult()).isEqualTo(PokerRoundResult.STAGE_1_FOLD);
            });
        }

        @Test
        void STAGE_2에서_폴드하면_STAGE_2_FOLD_결과를_가진다() {
            hand.fold(PokerPhase.STAGE_2);

            assertThat(hand.getFoldResult()).isEqualTo(PokerRoundResult.STAGE_2_FOLD);
        }

        @Test
        void 이미_폴드한_플레이어가_다시_폴드하면_예외가_발생한다() {
            hand.fold(PokerPhase.STAGE_1);

            assertCoffeeShoutException(
                    () -> hand.fold(PokerPhase.STAGE_2),
                    NumberPokerErrorCode.ALREADY_FOLDED
            );
        }
    }

    @Nested
    class 결과_결정 {

        @Test
        void 폴드한_경우_폴드_결과를_반환한다() {
            hand.fold(PokerPhase.STAGE_1);
            final HandRanking dealerRanking = HandRanking.of(new PokerCard(5), new PokerCard(5));

            assertThat(hand.determineResult(dealerRanking)).isEqualTo(PokerRoundResult.STAGE_1_FOLD);
        }

        @Test
        void 플레이어_패가_딜러보다_강하면_WIN이다() {
            // 플레이어: [8, 3] 하이카드 8, 딜러: [5, 4] 하이카드 5
            final HandRanking dealerRanking = HandRanking.of(new PokerCard(5), new PokerCard(4));

            assertThat(hand.determineResult(dealerRanking)).isEqualTo(PokerRoundResult.WIN);
        }

        @Test
        void 플레이어_패가_딜러보다_약하면_LOSE다() {
            // 플레이어: [8, 3] 하이카드 8, 딜러: [9, 9] 페어 9
            final HandRanking dealerRanking = HandRanking.of(new PokerCard(9), new PokerCard(9));

            assertThat(hand.determineResult(dealerRanking)).isEqualTo(PokerRoundResult.LOSE);
        }

        @Test
        void 패가_동일하면_TIE다() {
            // 플레이어: [8, 3], 딜러: [8, 3]
            final HandRanking dealerRanking = HandRanking.of(new PokerCard(8), new PokerCard(3));

            assertThat(hand.determineResult(dealerRanking)).isEqualTo(PokerRoundResult.TIE);
        }

        @Test
        void 카드_입력_순서가_달라도_같은_숫자_조합이면_TIE다() {
            // 플레이어: [3, 8], 딜러: [8, 3] → 동일 패 → TIE
            final PlayerPokerHand reversedHand = new PlayerPokerHand(꾹이, new PokerCard(3), new PokerCard(8));
            final HandRanking dealerRanking = HandRanking.of(new PokerCard(8), new PokerCard(3));

            assertThat(reversedHand.determineResult(dealerRanking)).isEqualTo(PokerRoundResult.TIE);
        }
    }

    @Nested
    class 핸드_랭킹 {

        @Test
        void 플레이어_패의_핸드_랭킹을_반환한다() {
            final PlayerPokerHand pairHand = new PlayerPokerHand(꾹이, new PokerCard(7), new PokerCard(7));

            assertThat(pairHand.getHandRanking().isPair()).isTrue();
        }
    }
}
