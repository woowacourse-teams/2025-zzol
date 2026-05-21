package coffeeshout.speedtouch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.player.PlayerName;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpeedTouchGameTest {

    private SpeedTouchGame game;
    private final PlayerName 한스 = new PlayerName("한스");
    private final PlayerName 꾹이 = new PlayerName("꾹이");
    private final PlayerName 루키 = new PlayerName("루키");

    @BeforeEach
    void setUp() {
        game = new SpeedTouchGame();
        game.setUp(List.of(new Gamer(한스, null), new Gamer(꾹이, null), new Gamer(루키, null)));
        game.startPlaying();
    }

    @Nested
    class 게임_상태 {

        @Test
        void 초기_상태는_DESCRIPTION이다() {
            // given
            final SpeedTouchGame newGame = new SpeedTouchGame();

            // when & then
            assertThat(newGame.getState()).isEqualTo(SpeedTouchGameState.DESCRIPTION);
        }

        @Test
        void startPlaying_호출시_PLAYING_상태가_된다() {
            // when & then
            assertThat(game.isPlaying()).isTrue();
            assertThat(game.getStartTime()).isNotNull();
        }

        @Test
        void getMiniGameType은_SPEED_TOUCH를_반환한다() {
            // when & then
            assertThat(game.getMiniGameType()).isEqualTo(MiniGameType.SPEED_TOUCH);
        }
    }

    @Nested
    class 터치_처리 {

        @Test
        void PLAYING_상태가_아니면_터치시_예외가_발생한다() {
            // given
            final SpeedTouchGame newGame = new SpeedTouchGame();
            newGame.setUp(List.of(new Gamer(한스, null)));

            // when & then
            assertThatThrownBy(() -> newGame.touch(new PlayerName("한스"), SpeedTouchPlayer.FIRST_NUMBER, Instant.now()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 올바른_번호_터치시_true를_반환한다() {
            // when
            final boolean result = game.touch(new PlayerName("한스"), SpeedTouchPlayer.FIRST_NUMBER, Instant.now());

            // then
            assertThat(result).isTrue();
        }

        @Test
        void 잘못된_번호_터치시_false를_반환한다() {
            // when
            final boolean result = game.touch(new PlayerName("한스"), 5, Instant.now());

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class 전원_완주_판정 {

        @Test
        void 한명이라도_완주하지_않으면_false이다() {
            // given
            finishPlayer("한스");
            finishPlayer("꾹이");

            // when & then
            assertThat(game.isAllFinished()).isFalse();
        }

        @Test
        void 전원_완주하면_true이다() {
            // given
            finishPlayer("한스");
            finishPlayer("꾹이");
            finishPlayer("루키");

            // when & then
            assertThat(game.isAllFinished()).isTrue();
        }
    }

    @Nested
    class 랭킹_산정 {

        @Test
        void 완주자는_소요시간이_짧은_순서대로_높은_순위를_받는다() {
            // given
            final Instant startTime = game.getStartTime();
            final Instant time1 = startTime.plusSeconds(10);
            final Instant time2 = startTime.plusSeconds(20);

            finishPlayerAt("한스", time1);
            finishPlayerAt("꾹이", time2);
            // 루키는 DNF

            // when
            final MiniGameResult result = game.getResult();

            // then - 한스(10초) > 꾹이(20초) > 루키(DNF)
            assertThat(result.getPlayerRank(한스)).isEqualTo(1);
            assertThat(result.getPlayerRank(꾹이)).isEqualTo(2);
            assertThat(result.getPlayerRank(루키)).isEqualTo(3);
        }

        @Test
        void DNF끼리는_진행도가_높은_사람이_더_높은_순위를_받는다() {
            // given - 전원 DNF, 진행도만 다름
            touchUpTo("한스", 20);
            touchUpTo("꾹이", 10);
            touchUpTo("루키", 5);

            // when
            final MiniGameResult result = game.getResult();

            // then
            assertThat(result.getPlayerRank(한스)).isEqualTo(1);
            assertThat(result.getPlayerRank(꾹이)).isEqualTo(2);
            assertThat(result.getPlayerRank(루키)).isEqualTo(3);
        }

        @Test
        void 완주자는_항상_DNF보다_높은_순위를_받는다() {
            // given
            final Instant finishTime = game.getStartTime().plusSeconds(29);
            finishPlayerAt("한스", finishTime); // 29초 완주
            touchUpTo("꾹이", SpeedTouchPlayer.LAST_NUMBER - 1); // 24/25 DNF
            touchUpTo("루키", SpeedTouchPlayer.FIRST_NUMBER); // 1/25 DNF

            // when
            final MiniGameResult result = game.getResult();

            // then
            assertThat(result.getPlayerRank(한스)).isEqualTo(1);
            assertThat(result.getPlayerRank(꾹이)).isEqualTo(2);
            assertThat(result.getPlayerRank(루키)).isEqualTo(3);
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
            assertThat(game.getState()).isEqualTo(SpeedTouchGameState.DONE);
        }

        @Test
        void tryFinish는_두번째_호출부터_false를_반환한다() {
            // given
            game.tryFinish();

            // when & then
            assertThat(game.tryFinish()).isFalse();
        }
    }

    private void finishPlayer(String name) {
        final Instant now = Instant.now();
        for (int i = SpeedTouchPlayer.FIRST_NUMBER; i <= SpeedTouchPlayer.LAST_NUMBER; i++) {
            game.touch(new PlayerName(name), i, now);
        }
    }

    private void finishPlayerAt(String name, Instant finishTime) {
        for (int i = SpeedTouchPlayer.FIRST_NUMBER; i <= SpeedTouchPlayer.LAST_NUMBER; i++) {
            game.touch(new PlayerName(name), i, finishTime);
        }
    }

    private void touchUpTo(String name, int upTo) {
        final Instant now = Instant.now();
        for (int i = SpeedTouchPlayer.FIRST_NUMBER; i <= upTo; i++) {
            game.touch(new PlayerName(name), i, now);
        }
    }
}
