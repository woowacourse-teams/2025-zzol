package coffeeshout.minigame.cardgame.domain.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.cardgame.domain.CardGameRound;
import coffeeshout.cardgame.domain.PlayerHands;
import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.MultiplierCard;
import coffeeshout.fixture.PlayerFixture;
import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Players;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerHandsTest {

    private PlayerHands playerHands;
    private Players players;

    @BeforeEach
    void setUp() {
        players = new Players("ABC23");
        players.join(PlayerFixture.호스트꾹이());
        players.join(PlayerFixture.호스트루키());
        players.join(PlayerFixture.호스트한스());
        players.join(PlayerFixture.호스트엠제이());

        playerHands = new PlayerHands(players.getPlayers());
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
            Player player = players.getPlayer(new PlayerName("꾹이"));
            Card card = AdditionCard.PLUS_40;

            // when
            playerHands.put(player, card);

            // then
            assertThat(playerHands.totalHandSize()).isEqualTo(1);
        }

        @Test
        void 여러_플레이어에게_카드를_추가한다() {
            // given
            Player player1 = players.getPlayer(new PlayerName("꾹이"));
            Player player2 = players.getPlayer(new PlayerName("루키"));

            // when
            playerHands.put(player1, AdditionCard.PLUS_40);
            playerHands.put(player2, AdditionCard.PLUS_30);

            // then
            assertThat(playerHands.totalHandSize()).isEqualTo(2);
        }

        @Test
        void 한_플레이어에게_여러_카드를_추가한다() {
            // given
            Player player = players.getPlayer(new PlayerName("꾹이"));

            // when
            playerHands.put(player, AdditionCard.PLUS_40);
            playerHands.put(player, MultiplierCard.DOUBLE);

            // then
            assertThat(playerHands.totalHandSize()).isEqualTo(2);
        }
    }

    @Nested
    class 라운드_종료_확인_테스트 {

        @Test
        void 카드가_없으면_라운드가_끝나지_않는다() {
            // when & then
            assertThat(playerHands.isRoundFinished()).isFalse();
        }

        @Test
        void 첫번째_라운드가_끝났는지_확인한다() {
            // given
            playerHands.put(players.getPlayer(new PlayerName("꾹이")), AdditionCard.PLUS_40);
            playerHands.put(players.getPlayer(new PlayerName("루키")), AdditionCard.PLUS_30);
            playerHands.put(players.getPlayer(new PlayerName("한스")), AdditionCard.PLUS_20);
            assertThat(playerHands.isRoundFinished()).isFalse();

            // when
            playerHands.put(players.getPlayer(new PlayerName("엠제이")), AdditionCard.PLUS_10);

            // then
            assertThat(playerHands.isRoundFinished()).isTrue();
        }

        @Test
        void 두번째_라운드가_끝났는지_확인한다() {
            // given - 첫 번째 라운드
            playerHands.put(players.getPlayer(new PlayerName("꾹이")), AdditionCard.PLUS_40);
            playerHands.put(players.getPlayer(new PlayerName("루키")), AdditionCard.PLUS_30);
            playerHands.put(players.getPlayer(new PlayerName("한스")), AdditionCard.PLUS_20);
            playerHands.put(players.getPlayer(new PlayerName("엠제이")), AdditionCard.PLUS_10);

            // when - 두 번째 라운드
            playerHands.put(players.getPlayer(new PlayerName("꾹이")), MultiplierCard.DOUBLE);
            playerHands.put(players.getPlayer(new PlayerName("루키")), MultiplierCard.QUADRUPLE);
            playerHands.put(players.getPlayer(new PlayerName("한스")), MultiplierCard.INVERT);
            assertThat(playerHands.isRoundFinished()).isFalse();
            playerHands.put(players.getPlayer(new PlayerName("엠제이")), AdditionCard.ZERO);

            // then
            assertThat(playerHands.isRoundFinished()).isTrue();
        }
    }

    @Nested
    class 플레이어_검색_테스트 {

        @Test
        void 이름으로_플레이어를_찾는다() {
            // given
            String playerName = "꾹이";

            // when
            Player foundPlayer = playerHands.findPlayerByName(new PlayerName(playerName));

            // then
            assertThat(foundPlayer.getName().value()).isEqualTo(playerName);
        }

        @Test
        void 존재하지_않는_플레이어_이름으로_검색하면_예외가_발생한다() {
            // given
            String nonExistentName = "존재하지않는플레이어";

            // when & then
            assertThatThrownBy(() -> playerHands.findPlayerByName(new PlayerName(nonExistentName)))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class 점수_계산_테스트 {

        @Test
        void 플레이어별_점수를_계산한다() {
            // given
            Player player1 = players.getPlayer(new PlayerName("꾹이"));
            Player player2 = players.getPlayer(new PlayerName("루키"));

            playerHands.put(player1, AdditionCard.PLUS_40);
            playerHands.put(player1, MultiplierCard.DOUBLE);
            playerHands.put(player2, AdditionCard.PLUS_30);
            playerHands.put(player2, MultiplierCard.INVERT);

            // when
            Map<Player, MiniGameScore> scores = playerHands.scoreByPlayer();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(scores.get(player1).getValue()).isEqualTo(80);
                softly.assertThat(scores.get(player2).getValue()).isEqualTo(-30);
                softly.assertThat(scores).hasSize(4);
            });
        }

        @Test
        void 카드가_없는_플레이어의_점수는_0이다() {
            // when
            Map<Player, MiniGameScore> scores = playerHands.scoreByPlayer();

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
            playerHands.put(players.getPlayer(new PlayerName("꾹이")), AdditionCard.PLUS_40);
            playerHands.put(players.getPlayer(new PlayerName("루키")), AdditionCard.PLUS_30);

            // when
            List<Player> unselectedPlayers = playerHands.getUnselectedPlayers(CardGameRound.FIRST);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(unselectedPlayers).hasSize(2);
                softly.assertThat(unselectedPlayers).contains(
                        players.getPlayer(new PlayerName("한스")),
                        players.getPlayer(new PlayerName("엠제이"))
                );
            });
        }

        @Test
        void 두번째_라운드에서_선택하지_않은_플레이어를_조회한다() {
            // given - 첫 번째 라운드 완료
            playerHands.put(players.getPlayer(new PlayerName("꾹이")), AdditionCard.PLUS_40);
            playerHands.put(players.getPlayer(new PlayerName("루키")), AdditionCard.PLUS_30);
            playerHands.put(players.getPlayer(new PlayerName("한스")), AdditionCard.PLUS_20);
            playerHands.put(players.getPlayer(new PlayerName("엠제이")), AdditionCard.PLUS_10);

            // 두 번째 라운드 일부 선택
            playerHands.put(players.getPlayer(new PlayerName("꾹이")), MultiplierCard.DOUBLE);

            // when
            List<Player> unselectedPlayers = playerHands.getUnselectedPlayers(CardGameRound.SECOND);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(unselectedPlayers).hasSize(3);
                softly.assertThat(unselectedPlayers).contains(
                        players.getPlayer(new PlayerName("루키")),
                        players.getPlayer(new PlayerName("한스")),
                        players.getPlayer(new PlayerName("엠제이"))
                );
            });
        }

        @Test
        void 모든_플레이어가_선택했으면_빈_리스트를_반환한다() {
            // given
            playerHands.put(players.getPlayer(new PlayerName("꾹이")), AdditionCard.PLUS_40);
            playerHands.put(players.getPlayer(new PlayerName("루키")), AdditionCard.PLUS_30);
            playerHands.put(players.getPlayer(new PlayerName("한스")), AdditionCard.PLUS_20);
            playerHands.put(players.getPlayer(new PlayerName("엠제이")), AdditionCard.PLUS_10);

            // when
            List<Player> unselectedPlayers = playerHands.getUnselectedPlayers(CardGameRound.FIRST);

            // then
            assertThat(unselectedPlayers).isEmpty();
        }
    }

    @Nested
    class 카드_소유자_조회_테스트 {

        @Test
        void 첫번째_라운드에서_카드_소유자를_찾는다() {
            // given
            Player player = players.getPlayer(new PlayerName("꾹이"));
            Card card = AdditionCard.PLUS_40;
            playerHands.put(player, card);

            // when
            Optional<Player> cardOwner = playerHands.findCardOwner(card, CardGameRound.FIRST);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardOwner).isPresent();
                softly.assertThat(cardOwner.get()).isEqualTo(player);
            });
        }

        @Test
        void 두번째_라운드에서_카드_소유자를_찾는다() {
            // given
            Player player = players.getPlayer(new PlayerName("꾹이"));
            playerHands.put(player, AdditionCard.PLUS_40); // 첫 번째 라운드
            Card secondRoundCard = MultiplierCard.DOUBLE;
            playerHands.put(player, secondRoundCard); // 두 번째 라운드

            // when
            Optional<Player> cardOwner = playerHands.findCardOwner(secondRoundCard, CardGameRound.SECOND);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardOwner).isPresent();
                softly.assertThat(cardOwner.get()).isEqualTo(player);
            });
        }

        @Test
        void 존재하지_않는_카드의_소유자를_찾으면_빈_Optional을_반환한다() {
            // given
            Card nonExistentCard = AdditionCard.PLUS_40;

            // when
            Optional<Player> cardOwner = playerHands.findCardOwner(nonExistentCard, CardGameRound.FIRST);

            // then
            assertThat(cardOwner).isEmpty();
        }

        @Test
        void 잘못된_라운드로_카드_소유자를_찾으면_빈_Optional을_반환한다() {
            // given
            Player player = players.getPlayer(new PlayerName("꾹이"));
            Card card = AdditionCard.PLUS_40;
            playerHands.put(player, card); // 첫 번째 라운드

            // when - 두 번째 라운드에서 첫 번째 라운드 카드를 찾음
            Optional<Player> cardOwner = playerHands.findCardOwner(card, CardGameRound.SECOND);

            // then
            assertThat(cardOwner).isEmpty();
        }
    }
}
