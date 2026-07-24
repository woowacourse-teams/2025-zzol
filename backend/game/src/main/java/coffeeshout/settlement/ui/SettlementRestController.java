package coffeeshout.settlement.ui;

import coffeeshout.gamecommon.SeasonUserProfileQuery;
import coffeeshout.gamecommon.SeasonUserProfileQuery.SeasonUserProfile;
import coffeeshout.settlement.application.SeasonLeaderboardService;
import coffeeshout.settlement.application.SeasonLeaderboardService.LeaderboardEntry;
import coffeeshout.settlement.domain.SeasonKey;
import coffeeshout.settlement.domain.SeasonTier;
import coffeeshout.settlement.ui.response.SeasonLeaderboardResponse;
import coffeeshout.settlement.ui.response.SeasonLeaderboardResponse.Row;
import coffeeshout.settlement.ui.response.SeasonRankResponse;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시즌 리더보드 조회 REST 엔드포인트. 순위·점수는 리더보드 ZSET에서, 표시 이름은
 * {@code :user}가 구현한 포트에서 읽는다. userId는 노출하지 않는다(ADR-0024).
 */
@RestController
@RequiredArgsConstructor
public class SettlementRestController {

    private static final int MAX_LIMIT = 100;

    private final SeasonLeaderboardService leaderboardService;
    private final SeasonUserProfileQuery userProfileQuery;
    private final Clock clock;

    @GetMapping("/settlement/leaderboard")
    public ResponseEntity<SeasonLeaderboardResponse> getLeaderboard(
            @RequestParam(required = false) String season,
            @RequestParam(defaultValue = "10") int limit
    ) {
        final String seasonKey = resolveSeason(season);
        final List<LeaderboardEntry> entries = leaderboardService.top(seasonKey, Math.min(limit, MAX_LIMIT));

        final Map<Long, SeasonUserProfile> profiles = userProfileQuery
                .resolveProfiles(entries.stream().map(LeaderboardEntry::userId).toList())
                .stream()
                .collect(Collectors.toMap(SeasonUserProfile::userId, Function.identity()));

        final List<Row> rows = new ArrayList<>();
        for (LeaderboardEntry entry : entries) {
            final SeasonUserProfile profile = profiles.get(entry.userId());
            // 탈퇴 회원은 프로필이 조회되지 않는다 — 리더보드에서 그 행을 숨긴다
            if (profile == null) {
                continue;
            }
            rows.add(new Row(
                    entry.rank(),
                    profile.nickname(),
                    profile.userCode(),
                    entry.totalPoints(),
                    SeasonTier.fromPoints(entry.totalPoints()).name()
            ));
        }
        return ResponseEntity.ok(
                new SeasonLeaderboardResponse(seasonKey, leaderboardService.memberCount(seasonKey), rows));
    }

    @GetMapping("/settlement/ranks")
    public ResponseEntity<SeasonRankResponse> getRank(
            @RequestParam String userCode,
            @RequestParam(required = false) String season
    ) {
        final String seasonKey = resolveSeason(season);

        final Optional<SeasonRankResponse> response = userProfileQuery.resolveUserIdByCode(userCode)
                .flatMap(userId -> leaderboardService.rankOf(seasonKey, userId)
                        .map(entry -> toRankResponse(seasonKey, userId, entry)));

        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private SeasonRankResponse toRankResponse(String seasonKey, long userId, LeaderboardEntry entry) {
        final SeasonUserProfile profile = userProfileQuery.resolveProfiles(List.of(userId)).stream()
                .findFirst()
                .orElse(new SeasonUserProfile(userId, "알 수 없음", ""));
        return new SeasonRankResponse(
                seasonKey,
                profile.nickname(),
                profile.userCode(),
                entry.rank(),
                entry.totalPoints(),
                SeasonTier.fromPoints(entry.totalPoints()).name(),
                leaderboardService.memberCount(seasonKey)
        );
    }

    private String resolveSeason(String season) {
        if (season != null && !season.isBlank()) {
            return season;
        }
        return SeasonKey.from(clock.instant()).value();
    }
}
