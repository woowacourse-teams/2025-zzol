package coffeeshout.minigame.cardgame.domain.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.CardGameRound;
import coffeeshout.cardgame.domain.CardGameState;
import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.CardGameDeckGenerator;
import coffeeshout.fixture.CardGameDeckStub;
import coffeeshout.fixture.CardGameFake;
import coffeeshout.fixture.PlayersFixture;
import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Players;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CardGameTest {

    CardGame cardGame;
    Players players;
    CardGameDeckGenerator deckGenerator = new CardGameDeckStub();

    @BeforeEach
    void setUp() {
        players = PlayersFixture.호스트꾹이_루키_엠제이_한스;

        cardGame = new CardGameFake(deckGenerator);
        cardGame.setUp(players.getPlayers());
    }

    @Nested
    class 게임_초기화_테스트 {

        @Test
        void 카드게임이_READY_상태로_시작한다() {
            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardGame.getRound()).isEqualTo(CardGameRound.READY);
                softly.assertThat(cardGame.getState()).isEqualTo(CardGameState.READY);
            });
        }

        @Test
        void 게임_시작시_첫번째_라운드로_진행한다() {
            // when
            cardGame.startRound();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardGame.getRound()).isEqualTo(CardGameRound.FIRST);
                softly.assertThat(cardGame.getState()).isEqualTo(CardGameState.FIRST_LOADING);
            });
        }

        @Test
        void 두번째_라운드_시작() {
            // given
            cardGame.startRound(); // FIRST 라운드

            // when
            cardGame.startRound(); // SECOND 라운드

            // then
            assertThat(cardGame.getRound()).isEqualTo(CardGameRound.SECOND);
        }
    }

    @Nested
    class 카드_선택_테스트 {

        @BeforeEach
        void setUp() {
            cardGame.startRound(); // FIRST 라운드 시작
        }

        @Test
        void 플레이어가_카드를_선택한다() {
            // given
            Player player = players.getPlayer(new PlayerName("꾹이"));

            // when
            cardGame.startPlay();
            cardGame.selectCard(player, 0);

            // then
            assertThat(cardGame.getPlayerHands().totalHandSize()).isEqualTo(1);
        }

        @Test
        void 게임이_진행중이_아닐때_카드_선택시_예외_발생() {
            // given
            cardGame.changeScoreBoardState(); // PLAYING이 아닌 상태로 변경
            Player player = players.getPlayer(new PlayerName("꾹이"));

            // when & then
            assertThatThrownBy(() -> cardGame.selectCard(player, 0))
                    .isInstanceOf(InvalidStateException.class);
        }

        @Test
        void 여러_플레이어가_카드를_선택한다() {
            // when
            cardGame.startPlay();
            cardGame.selectCard(players.getPlayer(new PlayerName("꾹이")), 0);
            cardGame.selectCard(players.getPlayer(new PlayerName("루키")), 1);
            cardGame.selectCard(players.getPlayer(new PlayerName("엠제이")), 2);

            // then
            assertThat(cardGame.getPlayerHands().totalHandSize()).isEqualTo(3);
        }

        @Test
        void 플레이어들이_같은_카드를_선택하면_예외를_반환한다() {
            // given
            cardGame.startPlay();
            Player player1 = players.getPlayer(new PlayerName("꾹이"));
            Player player2 = players.getPlayer(new PlayerName("루키"));

            // when
            cardGame.selectCard(player1, 0);

            // then
            assertThatThrownBy(() -> cardGame.selectCard(player2, 0))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class 점수_계산_테스트 {

        @BeforeEach
        void setUp() {
            cardGame.startRound();
        }

        @Test
        void 플레이어들의_점수를_계산한다() {
            // given
            Player player1 = players.getPlayer(new PlayerName("꾹이"));
            Player player2 = players.getPlayer(new PlayerName("루키"));
            Player player3 = players.getPlayer(new PlayerName("엠제이"));
            Player player4 = players.getPlayer(new PlayerName("한스"));

            cardGame.startPlay();
            cardGame.selectCard(player1, 0);
            cardGame.selectCard(player2, 1);
            cardGame.selectCard(player3, 2);
            cardGame.selectCard(player4, 3);

            // when
            Map<Player, MiniGameScore> scores = cardGame.getScores();

            // then - 점수가 계산되는지 확인 (shuffle에 의해 실제 값은 변할 수 있음)
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(scores).hasSize(4);
                softly.assertThat(scores.get(player1)).isNotNull();
                softly.assertThat(scores.get(player2)).isNotNull();
                softly.assertThat(scores.get(player3)).isNotNull();
                softly.assertThat(scores.get(player4)).isNotNull();
            });
        }

        @Test
        void 두_라운드를_진행할_수_있다() {
            // given
            Player player1 = players.getPlayer(new PlayerName("꾹이"));
            Player player2 = players.getPlayer(new PlayerName("루키"));
            Player player3 = players.getPlayer(new PlayerName("엠제이"));
            Player player4 = players.getPlayer(new PlayerName("한스"));

            // 첫 번째 라운드 완료
            cardGame.startPlay();
            cardGame.selectCard(player1, 0);
            cardGame.selectCard(player2, 1);
            cardGame.selectCard(player3, 2);
            cardGame.selectCard(player4, 3);

            // when - 두 번째 라운드 시작 가능한지 확인
            assertThat(cardGame.isFinished(CardGameRound.FIRST)).isTrue();

            // then - 점수가 계산되는지 확인
            Map<Player, MiniGameScore> scores = cardGame.getScores();
            assertThat(scores).hasSize(4);
        }
    }

    @Nested
    class 라운드_완료_확인_테스트 {

        @BeforeEach
        void setUp() {
            cardGame.startRound();
        }

        @Test
        void 첫번째_라운드가_완료되지_않음() {
            // given
            cardGame.startPlay();
            cardGame.selectCard(players.getPlayer(new PlayerName("꾹이")), 0);
            cardGame.selectCard(players.getPlayer(new PlayerName("루키")), 1);
            cardGame.selectCard(players.getPlayer(new PlayerName("엠제이")), 2);

            // when & then
            assertThat(cardGame.isFinished(CardGameRound.FIRST)).isFalse();
        }

        @Test
        void 첫번째_라운드가_완료됨() {
            // given
            cardGame.startPlay();
            cardGame.selectCard(players.getPlayer(new PlayerName("꾹이")), 0);
            cardGame.selectCard(players.getPlayer(new PlayerName("루키")), 1);
            cardGame.selectCard(players.getPlayer(new PlayerName("엠제이")), 2);
            cardGame.selectCard(players.getPlayer(new PlayerName("한스")), 3);

            // when & then
            assertThat(cardGame.isFinished(CardGameRound.FIRST)).isTrue();
        }

        @Test
        void 현재_라운드_완료_확인() {
            // given
            cardGame.startPlay();
            cardGame.selectCard(players.getPlayer(new PlayerName("꾹이")), 0);
            cardGame.selectCard(players.getPlayer(new PlayerName("루키")), 1);
            cardGame.selectCard(players.getPlayer(new PlayerName("엠제이")), 2);
            assertThat(cardGame.isFinishedThisRound()).isFalse();

            // when
            cardGame.selectCard(players.getPlayer(new PlayerName("한스")), 3);

            // then
            assertThat(cardGame.isFinishedThisRound()).isTrue();
        }

        @Test
        void 라운드_진행_상태를_확인한다() {
            // given - 첫 번째 라운드 완료
            cardGame.startPlay();
            cardGame.selectCard(players.getPlayer(new PlayerName("꾹이")), 0);
            cardGame.selectCard(players.getPlayer(new PlayerName("루키")), 1);
            cardGame.selectCard(players.getPlayer(new PlayerName("엠제이")), 2);
            assertThat(cardGame.isFinished(CardGameRound.FIRST)).isFalse();

            // when - 마지막 플레이어가 선택
            cardGame.selectCard(players.getPlayer(new PlayerName("한스")), 3);

            // then
            assertThat(cardGame.isFinished(CardGameRound.FIRST)).isTrue();
        }
    }

    @Nested
    class 플레이어_조회_테스트 {

        @Test
        void 이름으로_플레이어를_찾는다() {
            // given
            String playerName = "꾹이";

            // when
            Player foundPlayer = cardGame.findPlayerByName(new PlayerName(playerName));

            // then
            assertThat(foundPlayer.getName().value()).isEqualTo(playerName);
        }

        @Test
        void 존재하지_않는_플레이어_조회시_예외_발생() {
            // given
            String nonExistentName = "존재하지않는플레이어";

            PlayerName playerName = new PlayerName(nonExistentName);
            // when & then
            assertThatThrownBy(() -> cardGame.findPlayerByName(playerName))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class 라운드_상태_확인_테스트 {

        @Test
        void 첫번째_라운드_확인() {
            // given
            cardGame.startRound();

            // when & then
            assertThat(cardGame.getRound()).isEqualTo(CardGameRound.FIRST);
        }

        @Test
        void 두번째_라운드_확인() {
            // given
            cardGame.startRound(); // FIRST
            cardGame.startRound(); // SECOND

            // when & then
            assertThat(cardGame.getRound()).isEqualTo(CardGameRound.SECOND);
        }
    }

    @Nested
    class 랜덤_카드_할당_테스트 {

        @BeforeEach
        void setUp() {
            cardGame.startRound();
        }

        @Test
        void 선택하지_않은_플레이어에게_랜덤_카드_할당() {
            // given
            cardGame.startPlay();
            cardGame.selectCard(players.getPlayer(new PlayerName("꾹이")), 0);
            cardGame.selectCard(players.getPlayer(new PlayerName("루키")), 1);
            // players.getPlayer(new PlayerName("엠제이")), players.getPlayer(new PlayerName("한스"))은 선택하지 않음

            // when
            cardGame.assignRandomCardsToUnselectedPlayers();

            // then
            assertThat(cardGame.getPlayerHands().totalHandSize()).isEqualTo(4);
        }

        @Test
        void 모든_플레이어가_선택한_경우_변화_없음() {
            // given
            cardGame.startPlay();
            cardGame.selectCard(players.getPlayer(new PlayerName("꾹이")), 0);
            cardGame.selectCard(players.getPlayer(new PlayerName("루키")), 1);
            cardGame.selectCard(players.getPlayer(new PlayerName("엠제이")), 2);
            cardGame.selectCard(players.getPlayer(new PlayerName("한스")), 3);
            int sizeBefore = cardGame.getPlayerHands().totalHandSize();

            // when
            cardGame.assignRandomCardsToUnselectedPlayers();

            // then
            assertThat(cardGame.getPlayerHands().totalHandSize()).isEqualTo(sizeBefore);
        }
    }

    @Nested
    class 카드_소유자_조회_테스트 {

        @BeforeEach
        void setUp() {
            cardGame.startRound();
        }

        @Test
        void 현재_라운드의_카드_소유자를_찾는다() {
            // given
            cardGame.startPlay();
            Player player = players.getPlayer(new PlayerName("꾹이"));
            cardGame.selectCard(player, 0);
            Card selectedCard = cardGame.getDeck().getCards().getFirst();

            // when
            Optional<Player> cardOwner = cardGame.findCardOwnerInCurrentRound(selectedCard);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardOwner).isPresent();
                softly.assertThat(cardOwner.get()).isEqualTo(player);
            });
        }

        @Test
        void 선택되지_않은_카드의_소유자를_찾으면_빈_Optional_반환() {
            // given
            Card unselectedCard = cardGame.getDeck().getCards().get(5);

            // when
            Optional<Player> cardOwner = cardGame.findCardOwnerInCurrentRound(unselectedCard);

            // then
            assertThat(cardOwner).isEmpty();
        }
    }

    @Nested
    class 게임_상태_변경_테스트 {

        @Test
        void 점수판_상태로_변경() {
            // when
            cardGame.changeScoreBoardState();

            // then
            assertThat(cardGame.getState()).isEqualTo(CardGameState.SCORE_BOARD);
        }

        @Test
        void 라운드를_시작하면_로딩부터_시작된다() {
            // when
            cardGame.startRound();

            // then
            assertThat(cardGame.getState()).isEqualTo(CardGameState.FIRST_LOADING);
        }
    }

    @Nested
    class 게임_결과_테스트 {

        @BeforeEach
        void setUp() {
            cardGame.startRound();
        }

        @Test
        void 게임_결과를_반환한다() {
            // given
            cardGame.startPlay();
            cardGame.selectCard(players.getPlayer(new PlayerName("꾹이")), 0); // 40
            cardGame.selectCard(players.getPlayer(new PlayerName("루키")), 1); // 30
            cardGame.selectCard(players.getPlayer(new PlayerName("엠제이")), 2); // 20
            cardGame.selectCard(players.getPlayer(new PlayerName("한스")), 3); // 10

            // when
            var result = cardGame.getResult();

            // then
            assertThat(result).isNotNull();
        }
    }
}
