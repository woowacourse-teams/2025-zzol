package coffeeshout.blindtimer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.room.domain.player.PlayerName;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlindTimerPlayerTest {

    @Nested
    class STOP_처리 {

        @Test
        void STOP하면_경과시간이_기록된다() {
            // given
            final BlindTimerPlayer player = new BlindTimerPlayer(new PlayerName("한스"));

            // when
            final boolean result = player.stop(Duration.ofMillis(7500));

            // then
            assertThat(result).isTrue();
            assertThat(player.getStoppedElapsed()).isEqualTo(Duration.ofMillis(7500));
            assertThat(player.isStopped()).isTrue();
        }

        @Test
        void 이미_STOP한_플레이어는_다시_STOP할_수_없다() {
            // given
            final BlindTimerPlayer player = new BlindTimerPlayer(new PlayerName("한스"));
            player.stop(Duration.ofMillis(7500));

            // when
            final boolean result = player.stop(Duration.ofMillis(8000));

            // then
            assertThat(result).isFalse();
            assertThat(player.getStoppedElapsed()).isEqualTo(Duration.ofMillis(7500));
        }

        @Test
        void 타임아웃된_플레이어는_STOP할_수_없다() {
            // given
            final BlindTimerPlayer player = new BlindTimerPlayer(new PlayerName("한스"));
            player.markTimedOut();

            // when
            final boolean result = player.stop(Duration.ofMillis(10000));

            // then
            assertThat(result).isFalse();
            assertThat(player.isTimedOut()).isTrue();
        }
    }

    @Nested
    class 타임아웃 {

        @Test
        void 타임아웃_처리되면_timedOut이_true이다() {
            // given
            final BlindTimerPlayer player = new BlindTimerPlayer(new PlayerName("한스"));

            // when
            player.markTimedOut();

            // then
            assertThat(player.isTimedOut()).isTrue();
            assertThat(player.isStopped()).isTrue();
            assertThat(player.getStoppedElapsed()).isNull();
        }

        @Test
        void 이미_STOP한_플레이어는_타임아웃되지_않는다() {
            // given
            final BlindTimerPlayer player = new BlindTimerPlayer(new PlayerName("한스"));
            player.stop(Duration.ofMillis(7500));

            // when
            player.markTimedOut();

            // then
            assertThat(player.isTimedOut()).isFalse();
            assertThat(player.getStoppedElapsed()).isEqualTo(Duration.ofMillis(7500));
        }
    }

    @Nested
    class 초기_상태 {

        @Test
        void 생성_직후에는_멈추지_않은_상태이다() {
            // given
            final BlindTimerPlayer player = new BlindTimerPlayer(new PlayerName("한스"));

            // when & then
            assertThat(player.isStopped()).isFalse();
            assertThat(player.isTimedOut()).isFalse();
            assertThat(player.getStoppedElapsed()).isNull();
        }
    }
}
