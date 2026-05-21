package coffeeshout.blindtimer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.room.domain.player.PlayerName;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlindTimerPlayersTest {

    private BlindTimerPlayers players;
    private final Gamer 한스 = Gamer.guest(new PlayerName("한스"));
    private final Gamer 꾹이 = Gamer.guest(new PlayerName("꾹이"));

    @BeforeEach
    void setUp() {
        players = new BlindTimerPlayers(List.of(한스, 꾹이));
    }

    @Nested
    class 플레이어_조회 {

        @Test
        void 이름으로_플레이어를_찾을_수_있다() {
            // when
            final BlindTimerPlayer found = players.findByGamer(한스);

            // then
            assertThat(found.getGamer()).isEqualTo(한스);
        }

        @Test
        void 존재하지_않는_플레이어를_찾으면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> players.findByGamer(Gamer.guest(new PlayerName("없는사람"))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class 전원_STOP_판정 {

        @Test
        void 아무도_STOP하지_않으면_false이다() {
            // when & then
            assertThat(players.isAllStopped()).isFalse();
        }

        @Test
        void 한명만_STOP하면_false이다() {
            // given
            players.findByGamer(한스).stop(Duration.ofSeconds(7));

            // when & then
            assertThat(players.isAllStopped()).isFalse();
        }

        @Test
        void 전원_STOP하면_true이다() {
            // given
            players.findByGamer(한스).stop(Duration.ofSeconds(7));
            players.findByGamer(꾹이).stop(Duration.ofSeconds(8));

            // when & then
            assertThat(players.isAllStopped()).isTrue();
        }

        @Test
        void 타임아웃도_STOP으로_간주한다() {
            // given
            players.findByGamer(한스).stop(Duration.ofSeconds(7));
            players.findByGamer(꾹이).markTimedOut();

            // when & then
            assertThat(players.isAllStopped()).isTrue();
        }
    }
}
