package coffeeshout.speedtouch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.gamecommon.Gamer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpeedTouchPlayersTest {

    private Gamer 한스;
    private Gamer 꾹이;
    private SpeedTouchPlayers players;

    @BeforeEach
    void setUp() {
        한스 = PlayerFixture.게스트한스().toGamer();
        꾹이 = PlayerFixture.게스트꾹이().toGamer();
        players = new SpeedTouchPlayers(List.of(한스, 꾹이));
    }

    @Nested
    class 플레이어_조회 {

        @Test
        void 이름으로_플레이어를_찾을_수_있다() {
            // when
            final SpeedTouchPlayer found = players.findByName("한스");

            // then
            assertThat(found.getGamer()).isEqualTo(한스);
        }

        @Test
        void 존재하지_않는_이름으로_조회하면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> players.findByName("없는사람"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class 전원_완주_판정 {

        @Test
        void 아무도_완주하지_않으면_false를_반환한다() {
            // when & then
            assertThat(players.isAllFinished()).isFalse();
        }

        @Test
        void 일부만_완주하면_false를_반환한다() {
            // given
            final SpeedTouchPlayer 한스플레이어 = players.findByName("한스");
            final Instant now = Instant.now();
            for (int i = SpeedTouchPlayer.FIRST_NUMBER; i <= SpeedTouchPlayer.LAST_NUMBER; i++) {
                한스플레이어.touch(i, now);
            }

            // when & then
            assertThat(players.isAllFinished()).isFalse();
        }

        @Test
        void 전원_완주하면_true를_반환한다() {
            // given
            final Instant now = Instant.now();
            for (SpeedTouchPlayer player : players.getPlayers()) {
                for (int i = SpeedTouchPlayer.FIRST_NUMBER; i <= SpeedTouchPlayer.LAST_NUMBER; i++) {
                    player.touch(i, now);
                }
            }

            // when & then
            assertThat(players.isAllFinished()).isTrue();
        }
    }

    @Nested
    class 스트림_및_맵_변환 {

        @Test
        void stream으로_모든_플레이어를_순회할_수_있다() {
            // when
            final long count = players.stream().count();

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        void toPlayerMap으로_Player_키_맵을_생성할_수_있다() {
            // when
            final var map = players.toGamerMap();

            // then
            assertThat(map).hasSize(2);
            assertThat(map).containsKey(한스);
            assertThat(map).containsKey(꾹이);
        }
    }
}
