package coffeeshout.bombrelay.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BombRelayScoreTest {

    @Nested
    class 생존자_점수 {

        @Test
        void 생존자는_0점이다() {
            final BombRelayScore score = BombRelayScore.ofSurvivor();
            assertThat(score.getValue()).isEqualTo(0L);
        }
    }

    @Nested
    class 탈락자_점수 {

        @Test
        void 먼저_탈락할수록_점수가_높다() {
            // 3라운드 게임에서: 1라운드 탈락(3) > 2라운드 탈락(2) > 3라운드 탈락(1)
            final BombRelayScore round1 = BombRelayScore.ofEliminated(1, 3);
            final BombRelayScore round2 = BombRelayScore.ofEliminated(2, 3);
            final BombRelayScore round3 = BombRelayScore.ofEliminated(3, 3);

            assertThat(round1.getValue()).isGreaterThan(round2.getValue());
            assertThat(round2.getValue()).isGreaterThan(round3.getValue());
        }
    }

    @Nested
    class 정렬 {

        @Test
        void 오름차순_정렬시_생존자가_탈락자보다_앞에_온다() {
            final BombRelayScore survivor = BombRelayScore.ofSurvivor();
            final BombRelayScore eliminated = BombRelayScore.ofEliminated(1, 3);

            assertThat(survivor.compareTo(eliminated)).isLessThan(0);
        }

        @Test
        void 오름차순_정렬시_늦게_탈락한_사람이_앞에_온다() {
            // 3라운드 탈락(1) < 1라운드 탈락(3)
            final BombRelayScore lateElim = BombRelayScore.ofEliminated(3, 3);
            final BombRelayScore earlyElim = BombRelayScore.ofEliminated(1, 3);

            assertThat(lateElim.compareTo(earlyElim)).isLessThan(0);
        }
    }
}
