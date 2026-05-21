package coffeeshout.blockstacking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.room.domain.player.PlayerName;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlockStackingPlayerProgressTest {

    static final Gamer 플레이어 = new Gamer(new PlayerName("꾹이"), null);

    @Nested
    class 초기_생성_테스트 {

        @Test
        void initial_생성_시_floor가_0이다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(progress.gamer()).isEqualTo(플레이어);
                softly.assertThat(progress.currentFloor()).isZero();
                softly.assertThat(progress.failed()).isFalse();
            });
        }
    }

    @Nested
    class 실패_처리_테스트 {

        @Test
        void fail_호출_시_새_인스턴스를_반환한다() {
            final BlockStackingPlayerProgress original = BlockStackingPlayerProgress.initial(플레이어);

            final BlockStackingPlayerProgress failed = original.fail();

            assertThat(failed).isNotSameAs(original);
        }

        @Test
        void fail_호출_후_failed가_true이다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어).fail();

            assertThat(progress.failed()).isTrue();
        }

        @Test
        void fail_호출_후_gamer와_currentFloor는_유지된다() {
            final BlockStackingPlayerProgress original = BlockStackingPlayerProgress.initial(플레이어).advanceTo(3);

            final BlockStackingPlayerProgress failed = original.fail();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(failed.gamer()).isEqualTo(플레이어);
                softly.assertThat(failed.currentFloor()).isEqualTo(3);
            });
        }

        @Test
        void fail_호출_후_원본_인스턴스의_failed는_변경되지_않는다() {
            final BlockStackingPlayerProgress original = BlockStackingPlayerProgress.initial(플레이어);
            original.fail();

            assertThat(original.failed()).isFalse();
        }

        @Test
        void 실패_상태에서_advanceTo_호출_후_failed_상태가_유지된다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어)
                    .fail()
                    .advanceTo(1);

            assertThat(progress.failed()).isTrue();
        }
    }

    @Nested
    class 층수_진행_테스트 {

        @Test
        void advanceTo_호출_시_새_인스턴스를_반환한다() {
            final BlockStackingPlayerProgress original = BlockStackingPlayerProgress.initial(플레이어);

            final BlockStackingPlayerProgress advanced = original.advanceTo(5);

            assertThat(advanced).isNotSameAs(original);
        }

        @Test
        void advanceTo_호출_후_currentFloor가_갱신된다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어)
                    .advanceTo(3);

            assertThat(progress.currentFloor()).isEqualTo(3);
        }

        @Test
        void advanceTo_호출_후_gamer는_유지된다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어)
                    .advanceTo(7);

            assertThat(progress.gamer()).isEqualTo(플레이어);
        }

        @Test
        void 연속으로_advanceTo를_호출하면_마지막_floor가_유지된다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어)
                    .advanceTo(1)
                    .advanceTo(2)
                    .advanceTo(3);

            assertThat(progress.currentFloor()).isEqualTo(3);
        }

        @Test
        void 원본_인스턴스의_floor는_변경되지_않는다() {
            final BlockStackingPlayerProgress original = BlockStackingPlayerProgress.initial(플레이어);
            original.advanceTo(10);

            assertThat(original.currentFloor()).isZero();
        }
    }
}
