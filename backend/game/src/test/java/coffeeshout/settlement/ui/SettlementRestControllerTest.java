package coffeeshout.settlement.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.gamecommon.SeasonUserProfileQuery;
import coffeeshout.gamecommon.SeasonUserProfileQuery.SeasonUserProfile;
import coffeeshout.settlement.application.SeasonLeaderboardService;
import coffeeshout.settlement.application.SeasonLeaderboardService.LeaderboardEntry;
import coffeeshout.settlement.ui.response.SeasonLeaderboardResponse;
import coffeeshout.settlement.ui.response.SeasonRankResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettlementRestControllerTest {

    private static final String SEASON = "2026-07";

    @Mock
    SeasonLeaderboardService leaderboardService;
    @Mock
    SeasonUserProfileQuery userProfileQuery;

    private SettlementRestController controller;

    @BeforeEach
    void setUp() {
        // 시즌 기본값 검증을 위해 고정 시각(2026-07-15 KST) 사용
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        controller = new SettlementRestController(leaderboardService, userProfileQuery, fixedClock);
    }

    @Nested
    class 리더보드를_조회할_때 {

        @Test
        void 순위와_전역_식별자를_담아_반환하고_userId는_노출하지_않는다() {
            given(leaderboardService.top(SEASON, 10)).willReturn(List.of(
                    new LeaderboardEntry(1L, 1, 320),
                    new LeaderboardEntry(2L, 2, 100)
            ));
            given(leaderboardService.memberCount(SEASON)).willReturn(2L);
            given(userProfileQuery.resolveProfiles(anyList())).willReturn(List.of(
                    new SeasonUserProfile(1L, "한스", "AB123"),
                    new SeasonUserProfile(2L, "루키", "CD456")
            ));

            ResponseEntity<SeasonLeaderboardResponse> response = controller.getLeaderboard(null, 10);

            SeasonLeaderboardResponse body = response.getBody();
            assertThat(body.seasonKey()).isEqualTo(SEASON);
            assertThat(body.rows()).containsExactly(
                    new SeasonLeaderboardResponse.Row(1, "한스", "AB123", 320, "SILVER"),
                    new SeasonLeaderboardResponse.Row(2, "루키", "CD456", 100, "BRONZE")
            );
        }

        @Test
        void limit이_0_이하면_1로_보정한다() {
            // 하한 없이 0이 ZSET 범위(0, -1)로 흘러가면 Redis 규칙상 전체 조회가 된다
            given(leaderboardService.top(SEASON, 1)).willReturn(List.of());
            given(leaderboardService.memberCount(SEASON)).willReturn(0L);
            given(userProfileQuery.resolveProfiles(anyList())).willReturn(List.of());

            controller.getLeaderboard(SEASON, 0);
            controller.getLeaderboard(SEASON, -5);

            verify(leaderboardService, times(2)).top(SEASON, 1);
        }

        @Test
        void limit이_상한을_넘으면_100으로_보정한다() {
            given(leaderboardService.top(SEASON, 100)).willReturn(List.of());
            given(leaderboardService.memberCount(SEASON)).willReturn(0L);
            given(userProfileQuery.resolveProfiles(anyList())).willReturn(List.of());

            controller.getLeaderboard(SEASON, 500);

            verify(leaderboardService).top(SEASON, 100);
        }

        @Test
        void 탈퇴한_회원의_행은_숨긴다() {
            given(leaderboardService.top(SEASON, 10)).willReturn(List.of(
                    new LeaderboardEntry(1L, 1, 320),
                    new LeaderboardEntry(99L, 2, 100)
            ));
            given(leaderboardService.memberCount(SEASON)).willReturn(2L);
            // 탈퇴 회원(99L)은 프로필 조회에서 빠진다 (@SQLRestriction)
            given(userProfileQuery.resolveProfiles(anyList())).willReturn(List.of(
                    new SeasonUserProfile(1L, "한스", "AB123")
            ));

            ResponseEntity<SeasonLeaderboardResponse> response = controller.getLeaderboard(SEASON, 10);

            assertThat(response.getBody().rows()).hasSize(1);
        }
    }

    @Nested
    class 회원_순위를_조회할_때 {

        @Test
        void userCode로_시즌_순위를_반환한다() {
            given(userProfileQuery.resolveUserIdByCode("AB123")).willReturn(Optional.of(1L));
            given(leaderboardService.rankOf(SEASON, 1L)).willReturn(Optional.of(new LeaderboardEntry(1L, 3, 320)));
            given(leaderboardService.memberCount(SEASON)).willReturn(42L);
            given(userProfileQuery.resolveProfiles(List.of(1L)))
                    .willReturn(List.of(new SeasonUserProfile(1L, "한스", "AB123")));

            ResponseEntity<SeasonRankResponse> response = controller.getRank("AB123", SEASON);

            assertThat(response.getBody())
                    .isEqualTo(new SeasonRankResponse(SEASON, "한스", "AB123", 3, 320, "SILVER", 42L));
        }

        @Test
        void 존재하지_않는_userCode면_404를_반환한다() {
            given(userProfileQuery.resolveUserIdByCode("NOPE1")).willReturn(Optional.empty());

            ResponseEntity<SeasonRankResponse> response = controller.getRank("NOPE1", SEASON);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        void 이번_시즌_정산_이력이_없으면_404를_반환한다() {
            given(userProfileQuery.resolveUserIdByCode("AB123")).willReturn(Optional.of(1L));
            given(leaderboardService.rankOf(SEASON, 1L)).willReturn(Optional.empty());

            ResponseEntity<SeasonRankResponse> response = controller.getRank("AB123", SEASON);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }
}
