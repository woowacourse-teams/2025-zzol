package coffeeshout.numberpoker.domain;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NumberPokerGameTest {

    Player 꾹이 = PlayerFixture.호스트꾹이();
    Player 루키 = PlayerFixture.게스트루키();
    Player 엠제이 = PlayerFixture.게스트엠제이();
    List<Player> players;
    NumberPokerGame game;

    @BeforeEach
    void setUp() {
        players = List.of(꾹이, 루키, 엠제이);
        game = new NumberPokerGame(players);
    }

    @Nested
    class 초기_상태 {

        @Test
        void 게임은_LOADING_페이즈가_없는_대기_상태로_시작한다() {
            assertThat(game.getCurrentPhase()).isNull();
        }

        @Test
        void 기본_라운드_수는_3이다() {
            assertThat(game.getTotalRounds()).isEqualTo(3);
        }
    }

    @Nested
    class 라운드_수_설정 {

        @Test
        void 호스트가_라운드_수를_1에서_5_사이로_설정할_수_있다() {
            game.configureRoundCount(5);

            assertThat(game.getTotalRounds()).isEqualTo(5);
        }

        @Test
        void 범위_밖_라운드_수는_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> game.configureRoundCount(0),
                    NumberPokerErrorCode.INVALID_ROUND_COUNT
            );
            assertCoffeeShoutException(
                    () -> game.configureRoundCount(6),
                    NumberPokerErrorCode.INVALID_ROUND_COUNT
            );
        }
    }

    @Nested
    class 라운드_시작 {

        @Test
        void 라운드_시작_시_LOADING_페이즈가_된다() {
            game.startRound(new Random(42));

            assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.LOADING);
        }

        @Test
        void 라운드_시작_시_모든_플레이어에게_카드가_배분된다() {
            game.startRound(new Random(42));

            assertThat(game.hasCurrentRound()).isTrue();
        }

        @Test
        void 첫_번째_라운드임을_확인한다() {
            game.startRound(new Random(42));

            assertThat(game.isFirstRound()).isTrue();
        }
    }

    @Nested
    class 페이즈_전환 {

        @BeforeEach
        void 라운드_시작() {
            game.startRound(new Random(42));
        }

        @Test
        void LOADING에서_STAGE_1으로_전환한다() {
            game.beginStage1();

            assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.STAGE_1);
        }

        @Test
        void STAGE_1에서_STAGE_2로_전환한다() {
            game.beginStage1();
            game.beginStage2();

            assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.STAGE_2);
        }

        @Test
        void STAGE_2에서_SHOWDOWN으로_전환한다() {
            game.beginStage1();
            game.beginStage2();
            game.showdown();

            assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.SHOWDOWN);
        }

        @Test
        void SHOWDOWN에서_SCORE_BOARD로_전환한다() {
            game.beginStage1();
            game.beginStage2();
            game.showdown();
            game.scoreBoard();

            assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.SCORE_BOARD);
        }
    }

    @Nested
    class 폴드_처리 {

        @BeforeEach
        void 라운드_시작_후_STAGE_1() {
            game.startRound(new Random(42));
            game.beginStage1();
        }

        @Test
        void STAGE_1에서_플레이어가_폴드할_수_있다() {
            game.fold(꾹이);

            assertThat(game.isPlayerFolded(꾹이)).isTrue();
        }

        @Test
        void LOADING_페이즈에서_폴드하면_예외가_발생한다() {
            final NumberPokerGame newGame = new NumberPokerGame(players);
            newGame.startRound(new Random(42));

            assertCoffeeShoutException(
                    () -> newGame.fold(꾹이),
                    NumberPokerErrorCode.INVALID_PHASE_ACTION
            );
        }
    }

    @Nested
    class 전원_폴드_시_STAGE_2_스킵 {

        @Test
        void STAGE_1에서_전원_폴드하면_STAGE_2를_건너뛸_수_있다() {
            game.startRound(new Random(42));
            game.beginStage1();
            game.fold(꾹이);
            game.fold(루키);
            game.fold(엠제이);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(game.isAllFolded()).isTrue();
                softly.assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.STAGE_1);
            });
        }
    }

    @Nested
    class ROUND_READY {

        @BeforeEach
        void 첫_라운드_SCORE_BOARD까지_진행() {
            game.startRound(new Random(42));
            game.beginStage1();
            game.beginStage2();
            game.showdown();
            game.scoreBoard();
        }

        @Test
        void SCORE_BOARD_후_ROUND_READY로_전환한다() {
            game.beginRoundReady();

            assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.ROUND_READY);
        }

        @Test
        void 플레이어가_레디하면_상태가_기록된다() {
            game.beginRoundReady();
            game.markReady(꾹이);

            assertThat(game.isAllReady()).isFalse();
        }

        @Test
        void 전원_레디하면_true다() {
            game.beginRoundReady();
            game.markReady(꾹이);
            game.markReady(루키);
            game.markReady(엠제이);

            assertThat(game.isAllReady()).isTrue();
        }
    }

    @Nested
    class 마지막_라운드 {

        @Test
        void 마지막_라운드_SCORE_BOARD_후_ROUND_READY가_없다() {
            game.configureRoundCount(1);
            game.startRound(new Random(42));
            game.beginStage1();
            game.beginStage2();
            game.showdown();
            game.scoreBoard();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(game.isLastRound()).isTrue();
                softly.assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.SCORE_BOARD);
            });
        }
    }

    @Nested
    class 라운드_결과_조회 {

        @Test
        void SHOWDOWN_후_현재_라운드_결과를_조회할_수_있다() {
            game.startRound(new Random(42));
            game.beginStage1();
            game.beginStage2();
            game.showdown();

            final Map<Player, PokerRoundResult> results = game.getCurrentRoundResults();

            assertThat(results).hasSize(players.size());
        }
    }
}
