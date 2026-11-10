package coffeeshout.blockstacking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.gamecommon.Gamer;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlockStackingPlayerProgressTest {

    static final Gamer 플레이어명 = Gamer.guest("꾹이");

    @Nested
    class 초기_생성_테스트 {

        @Test
        void initial_생성_시_floor가_0이다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어명);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(progress.gamer()).isEqualTo(플레이어명);
                softly.assertThat(progress.currentFloor()).isZero();
                softly.assertThat(progress.failed()).isFalse();
            });
        }
    }

    @Nested
    class 실패_처리_테스트 {

        @Test
        void fail_호출_시_새_인스턴스를_반환한다() {
            final BlockStackingPlayerProgress original = BlockStackingPlayerProgress.initial(플레이어명);

            final BlockStackingPlayerProgress failed = original.fail();

            assertThat(failed).isNotSameAs(original);
        }

        @Test
        void fail_호출_후_failed가_true이다() {
            final BlockStackingPlayerProgress progress =
                    BlockStackingPlayerProgress.initial(플레이어명).fail();

            assertThat(progress.failed()).isTrue();
        }

        @Test
        void fail_호출_후_playerName과_currentFloor는_유지된다() {
            final BlockStackingPlayerProgress original =
                    BlockStackingPlayerProgress.initial(플레이어명).advanceTo(3, 85.0, 150.0);

            final BlockStackingPlayerProgress failed = original.fail();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(failed.gamer()).isEqualTo(플레이어명);
                softly.assertThat(failed.currentFloor()).isEqualTo(3);
            });
        }

        @Test
        void fail_호출_후_원본_인스턴스의_failed는_변경되지_않는다() {
            final BlockStackingPlayerProgress original = BlockStackingPlayerProgress.initial(플레이어명);
            original.fail();

            assertThat(original.failed()).isFalse();
        }

        @Test
        void 실패_상태에서_advanceTo_호출_후_failed_상태가_유지된다() {
            final BlockStackingPlayerProgress progress =
                    BlockStackingPlayerProgress.initial(플레이어명).fail().advanceTo(1, 85.0, 150.0);

            assertThat(progress.failed()).isTrue();
        }
    }

    @Nested
    class 층수_진행_테스트 {

        @Test
        void advanceTo_호출_시_새_인스턴스를_반환한다() {
            final BlockStackingPlayerProgress original = BlockStackingPlayerProgress.initial(플레이어명);

            final BlockStackingPlayerProgress advanced = original.advanceTo(5, 85.0, 150.0);

            assertThat(advanced).isNotSameAs(original);
        }

        @Test
        void advanceTo_호출_후_currentFloor가_갱신된다() {
            final BlockStackingPlayerProgress progress =
                    BlockStackingPlayerProgress.initial(플레이어명).advanceTo(3, 85.0, 150.0);

            assertThat(progress.currentFloor()).isEqualTo(3);
        }

        @Test
        void advanceTo_호출_후_playerName은_유지된다() {
            final BlockStackingPlayerProgress progress =
                    BlockStackingPlayerProgress.initial(플레이어명).advanceTo(7, 85.0, 150.0);

            assertThat(progress.gamer()).isEqualTo(플레이어명);
        }

        @Test
        void 연속으로_advanceTo를_호출하면_마지막_floor가_유지된다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어명)
                    .advanceTo(1, 85.0, 150.0)
                    .advanceTo(2, 85.0, 150.0)
                    .advanceTo(3, 85.0, 150.0);

            assertThat(progress.currentFloor()).isEqualTo(3);
        }

        @Test
        void initial은_쌓은_블록이_없어_top이_null이다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어명);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(progress.topX()).isNull();
                softly.assertThat(progress.topWidth()).isNull();
            });
        }

        @Test
        void advanceTo_호출_후_마지막_블록의_위치와_폭이_갱신된다() {
            final BlockStackingPlayerProgress progress = BlockStackingPlayerProgress.initial(플레이어명)
                    .advanceTo(1, 85.0, 150.0)
                    .advanceTo(2, 100.0, 135.0);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(progress.topX()).isEqualTo(100.0);
                softly.assertThat(progress.topWidth()).isEqualTo(135.0);
            });
        }

        @Test
        void fail_호출_후에도_마지막_블록은_유지된다() {
            final BlockStackingPlayerProgress failed = BlockStackingPlayerProgress.initial(플레이어명)
                    .advanceTo(1, 100.0, 135.0)
                    .fail();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(failed.topX()).isEqualTo(100.0);
                softly.assertThat(failed.topWidth()).isEqualTo(135.0);
            });
        }

        @Test
        void 원본_인스턴스의_floor는_변경되지_않는다() {
            final BlockStackingPlayerProgress original = BlockStackingPlayerProgress.initial(플레이어명);
            original.advanceTo(10, 85.0, 150.0);

            assertThat(original.currentFloor()).isZero();
        }
    }
}
