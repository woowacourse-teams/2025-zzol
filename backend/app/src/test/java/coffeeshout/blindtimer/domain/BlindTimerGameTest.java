package coffeeshout.blindtimer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.player.PlayerName;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlindTimerGameTest {

    private static final Duration TARGET_TIME = Duration.ofSeconds(10);

    private BlindTimerGame game;
    private final PlayerName 한스 = new PlayerName("한스");
    private final PlayerName 꾹이 = new PlayerName("꾹이");
    private final PlayerName 루키 = new PlayerName("루키");

    @BeforeEach
    void setUp() {
        game = new BlindTimerGame(TARGET_TIME);
        game.setUp(List.of(new Gamer(한스, null), new Gamer(꾹이, null), new Gamer(루키, null)));
        game.startPlaying();
    }

    @Nested
    class 게임_상태 {

        @Test
        void 초기_상태는_DESCRIPTION이다() {
            // given
            final BlindTimerGame newGame = new BlindTimerGame(TARGET_TIME);

            // when & then
            assertThat(newGame.getState()).isEqualTo(BlindTimerGameState.DESCRIPTION);
        }

        @Test
        void startPlaying_호출시_PLAYING_상태가_된다() {
            // when & then
            assertThat(game.isPlaying()).isTrue();
            assertThat(game.getStartTime()).isNotNull();
        }

        @Test
        void getMiniGameType은_BLIND_TIMER를_반환한다() {
            // when & then
            assertThat(game.getMiniGameType()).isEqualTo(MiniGameType.BLIND_TIMER);
        }

        @Test
        void 목표시간은_생성자에서_설정된_값이다() {
            // when & then
            assertThat(game.getTargetTime()).isEqualTo(TARGET_TIME);
        }

        @Test
        void 기본_생성자는_5초에서_19점99초_사이의_목표시간을_생성한다() {
            // given
            final BlindTimerGame randomGame = new BlindTimerGame();

            // when & then
            assertThat(randomGame.getTargetTime().toMillis()).isBetween(5000L, 19990L);
            assertThat(randomGame.getTargetTime().toMillis() % 10).isEqualTo(0L);
        }
    }

    @Nested
    class STOP_처리 {

        @Test
        void PLAYING_상태가_아니면_STOP시_예외가_발생한다() {
            // given
            final BlindTimerGame newGame = new BlindTimerGame(TARGET_TIME);
            newGame.setUp(List.of(new Gamer(한스, null)));

            // when & then
            assertThatThrownBy(() -> newGame.stop(new PlayerName("한스"), Instant.now()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 정상적으로_STOP하면_true를_반환한다() {
            // when
            final boolean result = game.stop(new PlayerName("한스"), Instant.now());

            // then
            assertThat(result).isTrue();
        }

        @Test
        void 이미_STOP한_플레이어가_다시_STOP하면_false를_반환한다() {
            // given
            game.stop(new PlayerName("한스"), Instant.now());

            // when
            final boolean result = game.stop(new PlayerName("한스"), Instant.now());

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class 전원_STOP_판정 {

        @Test
        void 한명이라도_STOP하지_않으면_false이다() {
            // given
            game.stop(new PlayerName("한스"), Instant.now());
            game.stop(new PlayerName("꾹이"), Instant.now());

            // when & then
            assertThat(game.isAllStopped()).isFalse();
        }

        @Test
        void 전원_STOP하면_true이다() {
            // given
            game.stop(new PlayerName("한스"), Instant.now());
            game.stop(new PlayerName("꾹이"), Instant.now());
            game.stop(new PlayerName("루키"), Instant.now());

            // when & then
            assertThat(game.isAllStopped()).isTrue();
        }
    }

    @Nested
    class 타임아웃 {

        @Test
        void markAllTimedOut은_STOP하지_않은_플레이어만_타임아웃시킨다() {
            // given
            game.stop(new PlayerName("한스"), Instant.now());

            // when
            game.markAllTimedOut();

            // then
            assertThat(game.findPlayer(new PlayerName("한스")).isTimedOut()).isFalse();
            assertThat(game.findPlayer(new PlayerName("꾹이")).isTimedOut()).isTrue();
            assertThat(game.findPlayer(new PlayerName("루키")).isTimedOut()).isTrue();
            assertThat(game.isAllStopped()).isTrue();
        }
    }

    @Nested
    class 랭킹_산정 {

        @Test
        void 오차가_작은_사람이_높은_순위를_받는다() {
            // given - 목표: 10.00초
            final Instant startTime = game.getStartTime();
            game.stop(new PlayerName("한스"), startTime.plusMillis(9800));   // 오차 200ms
            game.stop(new PlayerName("꾹이"), startTime.plusMillis(10500)); // 오차 500ms
            game.stop(new PlayerName("루키"), startTime.plusMillis(8000));  // 오차 2000ms

            // when
            final MiniGameResult result = game.getResult();

            // then
            assertThat(result.getPlayerRank(한스)).isEqualTo(1);
            assertThat(result.getPlayerRank(꾹이)).isEqualTo(2);
            assertThat(result.getPlayerRank(루키)).isEqualTo(3);
        }

        @Test
        void 정상_STOP은_항상_타임아웃보다_높은_순위를_받는다() {
            // given
            game.stop(new PlayerName("한스"), game.getStartTime().plusMillis(5000)); // 오차 5000ms
            game.markAllTimedOut(); // 꾹이, 루키 타임아웃

            // when
            final MiniGameResult result = game.getResult();

            // then
            assertThat(result.getPlayerRank(한스)).isEqualTo(1);
        }

        @Test
        void 전원_타임아웃이면_동순위이다() {
            // given
            game.markAllTimedOut();

            // when
            final MiniGameResult result = game.getResult();

            // then
            assertThat(result.getPlayerRank(한스)).isEqualTo(1);
            assertThat(result.getPlayerRank(꾹이)).isEqualTo(1);
            assertThat(result.getPlayerRank(루키)).isEqualTo(1);
        }
    }

    @Nested
    class 종료_원자성 {

        @Test
        void tryFinish는_처음_호출시_true를_반환하고_DONE_상태가_된다() {
            // when
            final boolean result = game.tryFinish();

            // then
            assertThat(result).isTrue();
            assertThat(game.getState()).isEqualTo(BlindTimerGameState.DONE);
        }

        @Test
        void tryFinish는_두번째_호출부터_false를_반환한다() {
            // given
            game.tryFinish();

            // when & then
            assertThat(game.tryFinish()).isFalse();
        }
    }
}
