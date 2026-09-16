package coffeeshout.admin.user.ui.response;

import coffeeshout.admin.user.domain.UserStats;
import java.time.LocalDate;
import java.util.List;

/**
 * 유저 화면 상단 그래프.
 *
 * <p>제공자 이름은 코드 그대로 보낸다. 한글 이름은 화면이 붙인다. 게임 이름과 반대인데,
 * 게임은 서버가 enum 을 늘리면 화면이 모르는 값이 생기는 반면 소셜 제공자는 셋이 고정이고
 * 그 셋의 색을 화면이 이미 들고 있어서다.
 */
public record UserStatsResponse(
        long userCount,
        List<ProviderSlice> providers,
        List<DailyPoint> signups,
        List<BucketSlice> playBuckets,
        List<BucketSlice> activityBuckets,
        long playedUserCount,
        long activeUserCount) {

    public static UserStatsResponse from(UserStats stats) {
        return new UserStatsResponse(
                stats.userCount(),
                stats.providers().stream()
                        .map(provider -> new ProviderSlice(provider.provider(), provider.count()))
                        .toList(),
                stats.signups().stream()
                        .map(point -> new DailyPoint(point.date(), point.count()))
                        .toList(),
                toSlices(stats.playBuckets()),
                toSlices(stats.activityBuckets()),
                stats.playedUserCount(),
                stats.activeUserCount());
    }

    private static List<BucketSlice> toSlices(List<UserStats.Bucket> buckets) {
        return buckets.stream()
                .map(bucket -> new BucketSlice(bucket.label(), bucket.count()))
                .toList();
    }

    public record ProviderSlice(String provider, long count) {}

    public record DailyPoint(LocalDate date, long count) {}

    public record BucketSlice(String label, long count) {}
}
