package coffeeshout.admin.room.application;

import coffeeshout.admin.overview.domain.GamePlayStat;
import coffeeshout.admin.overview.domain.OverviewStatisticsRepository;
import coffeeshout.admin.overview.domain.OverviewStatisticsRepository.GamePlayCount;
import coffeeshout.admin.room.domain.RoomLookupRepository;
import coffeeshout.admin.room.domain.RoomSnapshot;
import coffeeshout.admin.room.domain.RoomStats;
import coffeeshout.admin.room.domain.RoomStats.Bucket;
import coffeeshout.admin.room.domain.RoomStats.StatusCount;
import coffeeshout.room.domain.RoomState;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 화면 상단 그래프.
 *
 * <p>{@link RoomLookupService} 와 나눴다. 저쪽은 방 하나를 파고드는 일이고 이쪽은 방
 * 전체의 모양을 보는 일이다. 한 서비스에 두면 문의 대응 경로를 고칠 때마다 집계 쿼리를
 * 함께 읽어야 한다.
 *
 * <p>게임별 집계는 {@link OverviewStatisticsRepository} 것을 그대로 쓴다. 같은 질문에
 * 쿼리를 두 벌 두면 홈의 "게임별 비중"과 이 화면의 숫자가 언젠가 어긋나는데, 그때
 * 어느 쪽이 맞는지 아무도 모른다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomStatsService {

    /**
     * 인원 구간. 1명을 혼자 세운다.
     *
     * <p>방장이 만들고 아무도 안 들어온 방이다. 이 칸이 크면 초대 경로에 문제가 있다는
     * 뜻이고, 2~3인과 섞어 놓으면 그 사실이 "소규모 방이 많다"로 뭉개진다.
     */
    private static final List<PlayerBand> PLAYER_BANDS = List.of(
            new PlayerBand("1명", 1, 1),
            new PlayerBand("2~3명", 2, 3),
            new PlayerBand("4~5명", 4, 5),
            new PlayerBand("6~8명", 6, 8),
            new PlayerBand("9명 이상", 9, Long.MAX_VALUE));

    /**
     * 소요 시간 구간.
     *
     * <p>경계를 5, 10, 20, 30분에 둔다. 처음엔 1분과 3분에 칸을 두었는데 실제 방은 미니게임
     * 세 판에 룰렛까지 돌아 10분 안팎이 보통이었고, 그 결과 막대 하나가 "10분 이상"에
     * 전부 몰려 분포가 아무 말도 하지 않았다. <b>구간은 데이터가 실제로 퍼져 있는 폭에
     * 맞춰야</b> 칸이 구실을 한다.
     *
     * <p>30분 이상은 방이 열린 채 잊힌 것으로 본다. 이 칸이 커지면 방이 자동으로 닫히지
     * 않고 있다는 뜻이다.
     */
    private static final List<DurationBand> DURATION_BANDS = List.of(
            new DurationBand("5분 미만", 5),
            new DurationBand("5~10분", 10),
            new DurationBand("10~20분", 20),
            new DurationBand("20~30분", 30),
            new DurationBand("30분 이상", Long.MAX_VALUE));

    private final RoomLookupRepository roomLookupRepository;
    private final OverviewStatisticsRepository overviewStatisticsRepository;
    private final Clock clock;

    public RoomStats findStats(int days) {
        final LocalDate today = LocalDate.now(clock);
        final LocalDateTime from = today.minusDays(days - 1L).atStartOfDay();
        final LocalDateTime to = today.plusDays(1).atStartOfDay();

        final List<RoomSnapshot> snapshots = roomLookupRepository.findSnapshots(from, to);
        return new RoomStats(
                snapshots.size(),
                toGameStats(overviewStatisticsRepository.countPlaysByGame(from, to)),
                toPlayerBuckets(snapshots),
                toStatusCounts(snapshots),
                toDurationBuckets(snapshots),
                snapshots.stream()
                        .filter(snapshot -> snapshot.playerCount() == 1)
                        .count());
    }

    private static List<GamePlayStat> toGameStats(List<GamePlayCount> counts) {
        final long total = counts.stream().mapToLong(GamePlayCount::started).sum();
        return counts.stream()
                .sorted(Comparator.comparingLong(GamePlayCount::started).reversed())
                .map(count -> new GamePlayStat(
                        count.miniGameType(),
                        count.started(),
                        count.finished(),
                        total == 0 ? 0 : (double) count.started() / total))
                .toList();
    }

    private static List<Bucket> toPlayerBuckets(List<RoomSnapshot> snapshots) {
        return PLAYER_BANDS.stream()
                .map(band -> new Bucket(
                        band.label(),
                        snapshots.stream()
                                .filter(snapshot -> band.contains(snapshot.playerCount()))
                                .count()))
                .toList();
    }

    /**
     * 상태별 방 수. 방이 하나도 없는 상태도 0으로 보낸다.
     *
     * <p>빠진 상태를 화면이 알아서 채우게 두면 상태가 하나 늘었을 때 그 칸이 조용히
     * 사라진다. 순서도 여기서 정한다. {@link RoomState} 의 선언 순서가 곧 방이 거쳐 가는
     * 순서라서, 그대로 두면 막대가 왼쪽부터 진행 순으로 선다.
     */
    private static List<StatusCount> toStatusCounts(List<RoomSnapshot> snapshots) {
        final List<StatusCount> counts = new ArrayList<>();
        for (RoomState state : RoomState.values()) {
            counts.add(new StatusCount(
                    state,
                    snapshots.stream()
                            .filter(snapshot -> snapshot.status() == state)
                            .count()));
        }
        return counts;
    }

    private static List<Bucket> toDurationBuckets(List<RoomSnapshot> snapshots) {
        final List<RoomSnapshot> finished = snapshots.stream()
                .filter(snapshot -> snapshot.finishedAt() != null)
                .toList();

        final List<Bucket> buckets = new ArrayList<>();
        long lower = 0;
        for (DurationBand band : DURATION_BANDS) {
            final long from = lower;
            buckets.add(new Bucket(
                    band.label(),
                    finished.stream()
                            .filter(snapshot -> {
                                final long minutes = Duration.between(snapshot.createdAt(), snapshot.finishedAt())
                                        .toMinutes();
                                return minutes >= from && minutes < band.upperMinutes();
                            })
                            .count()));
            lower = band.upperMinutes();
        }
        return buckets;
    }

    private record PlayerBand(String label, long min, long max) {
        boolean contains(long players) {
            return players >= min && players <= max;
        }
    }

    private record DurationBand(String label, long upperMinutes) {}
}
