package coffeeshout.admin.room.ui.response;

import coffeeshout.admin.overview.ui.response.GamePlayStatResponse;
import coffeeshout.admin.room.domain.RoomStats;
import coffeeshout.room.domain.RoomState;
import java.util.List;

/**
 * 방 화면 상단 그래프.
 *
 * <p>게임 이름은 서버가 한글까지 붙여 내려보낸다({@link GamePlayStatResponse}). 홈의 게임별
 * 비중과 같은 응답을 쓰므로 두 화면이 같은 이름을 쓴다.
 */
public record RoomStatsResponse(
        long roomCount,
        List<GamePlayStatResponse> games,
        List<BucketSlice> playerBuckets,
        List<StatusSlice> statuses,
        List<BucketSlice> durationBuckets,
        long soloRoomCount) {

    public static RoomStatsResponse from(RoomStats stats) {
        return new RoomStatsResponse(
                stats.roomCount(),
                stats.games().stream().map(GamePlayStatResponse::from).toList(),
                toSlices(stats.playerBuckets()),
                stats.statuses().stream()
                        .map(status -> new StatusSlice(status.status(), status.count()))
                        .toList(),
                toSlices(stats.durationBuckets()),
                stats.soloRoomCount());
    }

    private static List<BucketSlice> toSlices(List<RoomStats.Bucket> buckets) {
        return buckets.stream()
                .map(bucket -> new BucketSlice(bucket.label(), bucket.count()))
                .toList();
    }

    public record BucketSlice(String label, long count) {}

    public record StatusSlice(RoomState status, long count) {}
}
