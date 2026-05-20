package coffeeshout.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserStatsTest {

    @Nested
    class 생성 {

        @Test
        void empty_팩토리는_winCount와_survivalStreak이_0으로_생성된다() {
            final UserStats stats = UserStats.empty(1L);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(stats.getUserId()).isEqualTo(1L);
                softly.assertThat(stats.getWinCount()).isZero();
                softly.assertThat(stats.getSurvivalStreak()).isZero();
            });
        }
    }

    @Nested
    class 당첨_기록 {

        @Test
        void recordWin_호출_시_winCount가_1_증가하고_survivalStreak이_0으로_리셋된다() {
            final UserStats stats = new UserStats(1L, 3, 5);

            stats.recordWin();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(stats.getWinCount()).isEqualTo(4);
                softly.assertThat(stats.getSurvivalStreak()).isZero();
            });
        }

        @Test
        void recordWin을_여러_번_호출하면_winCount가_누적된다() {
            final UserStats stats = UserStats.empty(1L);

            stats.recordWin();
            stats.recordWin();
            stats.recordWin();

            assertThat(stats.getWinCount()).isEqualTo(3);
        }

        @Test
        void survivalStreak이_쌓인_상태에서_recordWin_호출_시_streak이_리셋된다() {
            final UserStats stats = UserStats.empty(1L);
            stats.recordSurvival();
            stats.recordSurvival();

            stats.recordWin();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(stats.getWinCount()).isEqualTo(1);
                softly.assertThat(stats.getSurvivalStreak()).isZero();
            });
        }
    }

    @Nested
    class 생존_기록 {

        @Test
        void recordSurvival_호출_시_survivalStreak이_1_증가하고_winCount는_유지된다() {
            final UserStats stats = new UserStats(1L, 5, 2);

            stats.recordSurvival();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(stats.getWinCount()).isEqualTo(5);
                softly.assertThat(stats.getSurvivalStreak()).isEqualTo(3);
            });
        }

        @Test
        void recordSurvival을_여러_번_호출하면_survivalStreak이_누적된다() {
            final UserStats stats = UserStats.empty(1L);

            stats.recordSurvival();
            stats.recordSurvival();
            stats.recordSurvival();

            assertThat(stats.getSurvivalStreak()).isEqualTo(3);
        }
    }
}
