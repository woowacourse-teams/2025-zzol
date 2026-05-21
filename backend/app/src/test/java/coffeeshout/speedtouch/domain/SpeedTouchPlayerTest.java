package coffeeshout.speedtouch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.room.domain.player.PlayerName;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpeedTouchPlayerTest {

    @Nested
    class 터치_처리 {

        @Test
        void 올바른_번호를_터치하면_true를_반환한다() {
            // given
            final SpeedTouchPlayer player = new SpeedTouchPlayer(Gamer.guest(new PlayerName("한스")));

            // when
            final boolean result = player.touch(SpeedTouchPlayer.FIRST_NUMBER, Instant.now());

            // then
            assertThat(result).isTrue();
            assertThat(player.getCurrentNumber()).isEqualTo(SpeedTouchPlayer.FIRST_NUMBER + 1);
        }

        @Test
        void 잘못된_번호를_터치하면_false를_반환한다() {
            // given
            final SpeedTouchPlayer player = new SpeedTouchPlayer(Gamer.guest(new PlayerName("한스")));

            // when
            final boolean result = player.touch(3, Instant.now());

            // then
            assertThat(result).isFalse();
            assertThat(player.getCurrentNumber()).isEqualTo(SpeedTouchPlayer.FIRST_NUMBER);
        }

        @Test
        void 순서대로_처음부터_끝까지_터치하면_완주한다() {
            // given
            final SpeedTouchPlayer player = new SpeedTouchPlayer(Gamer.guest(new PlayerName("한스")));
            final Instant now = Instant.now();

            // when
            for (int i = SpeedTouchPlayer.FIRST_NUMBER; i <= SpeedTouchPlayer.LAST_NUMBER; i++) {
                player.touch(i, now);
            }

            // then
            assertThat(player.isFinished()).isTrue();
            assertThat(player.getFinishTime()).isNotNull();
        }

        @Test
        void 완주_후_추가_터치는_무시된다() {
            // given
            final SpeedTouchPlayer player = new SpeedTouchPlayer(Gamer.guest(new PlayerName("한스")));
            final Instant now = Instant.now();
            for (int i = SpeedTouchPlayer.FIRST_NUMBER; i <= SpeedTouchPlayer.LAST_NUMBER; i++) {
                player.touch(i, now);
            }

            // when
            final boolean result = player.touch(SpeedTouchPlayer.LAST_NUMBER, now);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class 진행도 {

        @Test
        void 초기_진행도는_0이다() {
            // given
            final SpeedTouchPlayer player = new SpeedTouchPlayer(Gamer.guest(new PlayerName("한스")));

            // when & then
            assertThat(player.getProgress()).isEqualTo(0);
        }

        @Test
        void 다섯번까지_터치하면_진행도는_5이다() {
            // given
            final SpeedTouchPlayer player = new SpeedTouchPlayer(Gamer.guest(new PlayerName("한스")));
            final Instant now = Instant.now();
            for (int i = SpeedTouchPlayer.FIRST_NUMBER; i <= 5; i++) {
                player.touch(i, now);
            }

            // when & then
            assertThat(player.getProgress()).isEqualTo(5);
        }
    }

    @Nested
    class 완주시간_계산 {

        @Test
        void 완주한_플레이어의_소요시간을_밀리초로_계산한다() {
            // given
            final SpeedTouchPlayer player = new SpeedTouchPlayer(Gamer.guest(new PlayerName("한스")));
            final Instant startTime = Instant.parse("2025-01-01T00:00:00Z");
            final Instant finishTime = Instant.parse("2025-01-01T00:00:15.500Z");
            for (int i = SpeedTouchPlayer.FIRST_NUMBER; i <= SpeedTouchPlayer.LAST_NUMBER; i++) {
                player.touch(i, finishTime);
            }

            // when
            final long millis = player.calculateFinishMillis(startTime);

            // then
            assertThat(millis).isEqualTo(15500L);
        }

        @Test
        void 완주하지_않은_플레이어의_소요시간_계산은_예외가_발생한다() {
            // given
            final SpeedTouchPlayer player = new SpeedTouchPlayer(Gamer.guest(new PlayerName("한스")));
            final Instant startTime = Instant.now();

            // when & then
            assertThatThrownBy(() -> player.calculateFinishMillis(startTime))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
