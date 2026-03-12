package coffeeshout.blindtimer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlindTimerPlayersTest {

    private BlindTimerPlayers players;
    private Player 한스;
    private Player 꾹이;

    @BeforeEach
    void setUp() {
        한스 = PlayerFixture.게스트한스();
        꾹이 = PlayerFixture.게스트꾹이();
        players = new BlindTimerPlayers(List.of(한스, 꾹이));
    }

    @Nested
    class 플레이어_조회 {

        @Test
        void 이름으로_플레이어를_찾을_수_있다() {
            // when
            final BlindTimerPlayer found = players.findByName(new PlayerName("한스"));

            // then
            assertThat(found.getPlayer()).isEqualTo(한스);
        }

        @Test
        void 존재하지_않는_플레이어를_찾으면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> players.findByName(new PlayerName("없는사람")))
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
            players.findByName(new PlayerName("한스")).stop(7000L);

            // when & then
            assertThat(players.isAllStopped()).isFalse();
        }

        @Test
        void 전원_STOP하면_true이다() {
            // given
            players.findByName(new PlayerName("한스")).stop(7000L);
            players.findByName(new PlayerName("꾹이")).stop(8000L);

            // when & then
            assertThat(players.isAllStopped()).isTrue();
        }

        @Test
        void 타임아웃도_STOP으로_간주한다() {
            // given
            players.findByName(new PlayerName("한스")).stop(7000L);
            players.findByName(new PlayerName("꾹이")).markTimedOut();

            // when & then
            assertThat(players.isAllStopped()).isTrue();
        }
    }
}
