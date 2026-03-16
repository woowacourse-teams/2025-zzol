package coffeeshout.bombrelay.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BombRelayPlayerTest {

    @Nested
    class 탈락_처리 {

        @Test
        void 탈락시키면_eliminated가_true이고_라운드가_기록된다() {
            // given
            final BombRelayPlayer player = new BombRelayPlayer(PlayerFixture.게스트한스());

            // when
            player.eliminate(2);

            // then
            assertThat(player.isEliminated()).isTrue();
            assertThat(player.getEliminatedRound()).isEqualTo(2);
        }

        @Test
        void 초기_상태에서는_탈락이_아니다() {
            // given
            final BombRelayPlayer player = new BombRelayPlayer(PlayerFixture.게스트한스());

            // when & then
            assertThat(player.isEliminated()).isFalse();
            assertThat(player.getEliminatedRound()).isEqualTo(0);
        }
    }

    @Nested
    class 이름_조회 {

        @Test
        void getName은_Player의_이름을_반환한다() {
            // given
            final BombRelayPlayer player = new BombRelayPlayer(PlayerFixture.게스트한스());

            // when & then
            assertThat(player.getName()).isEqualTo("한스");
        }
    }
}
