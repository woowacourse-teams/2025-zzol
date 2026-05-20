package coffeeshout.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.ServiceTest;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserStats;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserStatsServiceTest extends ServiceTest {

    @Autowired
    UserStatsService userStatsService;

    @Autowired
    UserRegistrationService userRegistrationService;

    private Long userId;

    @BeforeEach
    void setUp() {
        final User user = userRegistrationService.registerOrLogin(
                OAuthProvider.GOOGLE, "google-uid-stats", "stats@example.com", "통계유저").user();
        userId = user.getId();
    }

    @Nested
    class 통계_조회 {

        @Test
        void 통계가_없는_사용자는_winCount와_survivalStreak이_0으로_조회된다() {
            final UserStats stats = userStatsService.getStats(userId);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(stats.getWinCount()).isZero();
                softly.assertThat(stats.getSurvivalStreak()).isZero();
            });
        }
    }

    @Nested
    class 당첨_업데이트 {

        @Test
        void isWinner가_true이면_winCount가_증가하고_survivalStreak이_0으로_리셋된다() {
            userStatsService.updateStats(userId, false);
            userStatsService.updateStats(userId, false);

            final UserStats result = userStatsService.updateStats(userId, true);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getWinCount()).isEqualTo(1);
                softly.assertThat(result.getSurvivalStreak()).isZero();
            });
        }

        @Test
        void isWinner가_true를_여러_번_호출하면_winCount가_누적된다() {
            userStatsService.updateStats(userId, true);
            userStatsService.updateStats(userId, true);
            final UserStats result = userStatsService.updateStats(userId, true);

            assertThat(result.getWinCount()).isEqualTo(3);
        }
    }

    @Nested
    class 생존_업데이트 {

        @Test
        void isWinner가_false이면_survivalStreak이_증가하고_winCount는_유지된다() {
            userStatsService.updateStats(userId, true);

            final UserStats result = userStatsService.updateStats(userId, false);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getWinCount()).isEqualTo(1);
                softly.assertThat(result.getSurvivalStreak()).isEqualTo(1);
            });
        }

        @Test
        void isWinner가_false를_여러_번_호출하면_survivalStreak이_누적된다() {
            userStatsService.updateStats(userId, false);
            userStatsService.updateStats(userId, false);
            final UserStats result = userStatsService.updateStats(userId, false);

            assertThat(result.getSurvivalStreak()).isEqualTo(3);
        }
    }
}
