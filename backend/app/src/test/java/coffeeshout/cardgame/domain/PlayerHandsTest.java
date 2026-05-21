package coffeeshout.cardgame.domain;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.MultiplierCard;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerHandsTest {

    private static final CardGameRound ROUND_FIRST = CardGameRound.roundOf(1, 2);
    private static final CardGameRound ROUND_SECOND = CardGameRound.roundOf(2, 2);

    private PlayerHands playerHands;
    private Gamer 꾹이;
    private Gamer 루키;
    private Gamer 한스;
    private Gamer 엠제이;

    @BeforeEach
    void setUp() {
        꾹이 = GamerFixture.게스트꾹이();
        루키 = GamerFixture.게스트루키();
        한스 = GamerFixture.게스트한스();
        엠제이 = GamerFixture.게스트엠제이();

        playerHands = new PlayerHands(List.of(꾹이, 루키, 한스, 엠제이));
    }

    @Nested
    class 플레이어_핸드_생성_테스트 {

        @Test
        void 플레이어_핸드가_생성된다() {
            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(playerHands.playerCount()).isEqualTo(4);
                softly.assertThat(playerHands.totalHandSize()).isEqualTo(0);
            });
        }
    }

    @Nested
    class 카드_추가_테스트 {

        @Test
        void 플레이어에게_카드를_추가한다() {
            // given
            Card card = AdditionCard.PLUS_40;

            // when
            playerHands.putByGamer(꾹이, card);

            // then
            assertThat(playerHands.totalHandSize()).isEqualTo(1);
        }

        @Test
        void 여러_플레이어에게_카드를_추가한다() {
            // when
            playerHands.putByGamer(꾹이, AdditionCard.PLUS_40);
            playerHands.putByGamer(루키, AdditionCard.PLUS_30);

            // then
            assertThat(playerHands.totalHandSize()).isEqualTo(2);
        }

        @Test
        void 한_플레이어에게_여러_카드를_추가한다() {
            // when
            playerHands.putByGamer(꾹이, AdditionCard.PLUS_40);
            playerHands.putByGamer(꾹이, MultiplierCard.DOUBLE);

            // then
            assertThat(playerHands.totalHandSize()).isEqualTo(2);
        }
    }

    @Nested
    class 라운드_종료_확인_테스트 {

        @Test
        void 카드가_없으면_라운드가_끝나지_않는다() {
            // when & then
            assertThat(playerHands.isRoundFinished(ROUND_FIRST)).isFalse();
        }

        @Test
        void 첫번째_라운드가_끝났는지_확인한다() {
            // given
            playerHands.putByGamer(꾹이, AdditionCard.PLUS_40);
            playerHands.putByGamer(루키, AdditionCard.PLUS_30);
            playerHands.putByGamer(한스, AdditionCard.PLUS_20);
            assertThat(playerHands.isRoundFinished(ROUND_FIRST)).isFalse();

            // when
            playerHands.putByGamer(엠제이, AdditionCard.PLUS_10);

            // then
            assertThat(playerHands.isRoundFinished(ROUND_FIRST)).isTrue();
        }

        @Test
        void 두번째_라운드가_끝났는지_확인한다() {
            // given - 첫 번째 라운드
            playerHands.putByGamer(꾹이, AdditionCard.PLUS_40);
            playerHands.putByGamer(루키, AdditionCard.PLUS_30);
            playerHands.putByGamer(한스, AdditionCard.PLUS_20);
            playerHands.putByGamer(엠제이, AdditionCard.PLUS_10);

            // when - 두 번째 라운드
            playerHands.putByGamer(꾹이, MultiplierCard.DOUBLE);
            playerHands.putByGamer(루키, MultiplierCard.QUADRUPLE);
            playerHands.putByGamer(한스, MultiplierCard.INVERT);
            assertThat(playerHands.isRoundFinished(ROUND_SECOND)).isFalse();
            playerHands.putByGamer(엠제이, AdditionCard.ZERO);

            // then
            assertThat(playerHands.isRoundFinished(ROUND_SECOND)).isTrue();
        }
    }

    @Nested
    class 플레이어_존재_확인_테스트 {

        @Test
        void 등록된_플레이어는_true를_반환한다() {
            assertThat(playerHands.containsGamer(꾹이)).isTrue();
        }

        @Test
        void 존재하지_않는_플레이어_이름으로_확인하면_false를_반환한다() {
            assertThat(playerHands.containsGamer(Gamer.guest(new PlayerName("존재하지않는플레이어")))).isFalse();
        }
    }

    @Nested
    class 점수_계산_테스트 {

        @Test
        void 플레이어별_점수를_계산한다() {
            // given
            playerHands.putByGamer(꾹이, AdditionCard.PLUS_40);
            playerHands.putByGamer(꾹이, MultiplierCard.DOUBLE);
            playerHands.putByGamer(루키, AdditionCard.PLUS_30);
            playerHands.putByGamer(루키, MultiplierCard.INVERT);

            // when
            Map<Gamer, MiniGameScore> scores = playerHands.scoreByGamer();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(scores.get(꾹이).getValue()).isEqualTo(80);
                softly.assertThat(scores.get(루키).getValue()).isEqualTo(-30);
                softly.assertThat(scores).hasSize(4);
            });
        }

        @Test
        void 카드가_없는_플레이어의_점수는_0이다() {
            // when
            Map<Gamer, MiniGameScore> scores = playerHands.scoreByGamer();

            // then
            scores.values().forEach(score ->
                    assertThat(score.getValue()).isZero()
            );
        }
    }

    @Nested
    class 선택하지_않은_플레이어_조회_테스트 {

        @Test
        void 첫번째_라운드에서_선택하지_않은_플레이어를_조회한다() {
            // given
            playerHands.putByGamer(꾹이, AdditionCard.PLUS_40);
            playerHands.putByGamer(루키, AdditionCard.PLUS_30);

            // when
            List<Gamer> unselectedGamers = playerHands.getUnselectedGamers(ROUND_FIRST);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(unselectedGamers).hasSize(2);
                softly.assertThat(unselectedGamers).contains(한스, 엠제이);
            });
        }

        @Test
        void 두번째_라운드에서_선택하지_않은_플레이어를_조회한다() {
            // given - 첫 번째 라운드 완료
            playerHands.putByGamer(꾹이, AdditionCard.PLUS_40);
            playerHands.putByGamer(루키, AdditionCard.PLUS_30);
            playerHands.putByGamer(한스, AdditionCard.PLUS_20);
            playerHands.putByGamer(엠제이, AdditionCard.PLUS_10);

            // 두 번째 라운드 일부 선택
            playerHands.putByGamer(꾹이, MultiplierCard.DOUBLE);

            // when
            List<Gamer> unselectedGamers = playerHands.getUnselectedGamers(ROUND_SECOND);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(unselectedGamers).hasSize(3);
                softly.assertThat(unselectedGamers).contains(루키, 한스, 엠제이);
            });
        }

        @Test
        void 모든_플레이어가_선택했으면_빈_리스트를_반환한다() {
            // given
            playerHands.putByGamer(꾹이, AdditionCard.PLUS_40);
            playerHands.putByGamer(루키, AdditionCard.PLUS_30);
            playerHands.putByGamer(한스, AdditionCard.PLUS_20);
            playerHands.putByGamer(엠제이, AdditionCard.PLUS_10);

            // when
            List<Gamer> unselectedGamers = playerHands.getUnselectedGamers(ROUND_FIRST);

            // then
            assertThat(unselectedGamers).isEmpty();
        }
    }

    @Nested
    class 카드_소유자_조회_테스트 {

        @Test
        void 첫번째_라운드에서_카드_소유자를_찾는다() {
            // given
            Card card = AdditionCard.PLUS_40;
            playerHands.putByGamer(꾹이, card);

            // when
            Optional<Gamer> cardOwner = playerHands.findCardOwner(card, ROUND_FIRST);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardOwner).isPresent();
                softly.assertThat(cardOwner.get()).isEqualTo(꾹이);
            });
        }

        @Test
        void 두번째_라운드에서_카드_소유자를_찾는다() {
            // given
            playerHands.putByGamer(꾹이, AdditionCard.PLUS_40); // 첫 번째 라운드
            Card secondRoundCard = MultiplierCard.DOUBLE;
            playerHands.putByGamer(꾹이, secondRoundCard); // 두 번째 라운드

            // when
            Optional<Gamer> cardOwner = playerHands.findCardOwner(secondRoundCard, ROUND_SECOND);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardOwner).isPresent();
                softly.assertThat(cardOwner.get()).isEqualTo(꾹이);
            });
        }

        @Test
        void 존재하지_않는_카드의_소유자를_찾으면_빈_Optional을_반환한다() {
            // given
            Card nonExistentCard = AdditionCard.PLUS_40;

            // when
            Optional<Gamer> cardOwner = playerHands.findCardOwner(nonExistentCard, ROUND_FIRST);

            // then
            assertThat(cardOwner).isEmpty();
        }

        @Test
        void 잘못된_라운드로_카드_소유자를_찾으면_빈_Optional을_반환한다() {
            // given
            Card card = AdditionCard.PLUS_40;
            playerHands.putByGamer(꾹이, card); // 첫 번째 라운드

            // when - 두 번째 라운드에서 첫 번째 라운드 카드를 찾음
            Optional<Gamer> cardOwner = playerHands.findCardOwner(card, ROUND_SECOND);

            // then
            assertThat(cardOwner).isEmpty();
        }
    }
}
