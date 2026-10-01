package coffeeshout.admin.room.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import coffeeshout.admin.overview.domain.GamePlayStat;
import coffeeshout.admin.overview.domain.OverviewStatisticsRepository;
import coffeeshout.admin.overview.domain.OverviewStatisticsRepository.GamePlayCount;
import coffeeshout.admin.room.domain.RoomLookupRepository;
import coffeeshout.admin.room.domain.RoomSnapshot;
import coffeeshout.admin.room.domain.RoomStats;
import coffeeshout.admin.room.domain.RoomStats.Bucket;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.RoomState;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@DisplayName("RoomStatsService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomStatsServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), KST);
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 12, 9, 0);

    @Mock
    private RoomLookupRepository roomLookupRepository;

    @Mock
    private OverviewStatisticsRepository overviewStatisticsRepository;

    private RoomStatsService roomStatsService;

    private void stub(List<RoomSnapshot> snapshots) {
        given(roomLookupRepository.findSnapshots(any(), any())).willReturn(snapshots);
        given(overviewStatisticsRepository.countPlaysByGame(any(), any())).willReturn(List.of());
        roomStatsService = new RoomStatsService(roomLookupRepository, overviewStatisticsRepository, CLOCK);
    }

    private static RoomSnapshot room(long players) {
        return new RoomSnapshot(RoomState.DONE, BASE, BASE.plusMinutes(2), players);
    }

    private static RoomSnapshot lasting(long minutes) {
        return new RoomSnapshot(RoomState.DONE, BASE, BASE.plusMinutes(minutes), 3);
    }

    private static long countOf(List<Bucket> buckets, String label) {
        return buckets.stream()
                .filter(bucket -> bucket.label().equals(label))
                .mapToLong(Bucket::count)
                .sum();
    }

    @Nested
    class 인원_분포 {

        @Test
        void 구간별로_방을_나눈다() {
            stub(List.of(room(1), room(2), room(3), room(5), room(12)));

            final List<Bucket> buckets = roomStatsService.findStats(30).playerBuckets();

            assertThat(buckets)
                    .extracting(Bucket::label, Bucket::count)
                    .containsExactly(
                            tuple("1명", 1L),
                            tuple("2~3명", 2L),
                            tuple("4~5명", 1L),
                            tuple("6~8명", 0L),
                            tuple("9명 이상", 1L));
        }

        @Test
        void 모든_방이_정확히_한_칸에만_들어간다() {
            // 구간 경계가 겹치거나 벌어지면 칸의 합이 방 수와 어긋난다. 어긋난 그래프는
            // 눈으로 알아채기 어렵고, 알아챈 뒤에도 어느 칸이 틀렸는지 모른다.
            final List<RoomSnapshot> snapshots =
                    List.of(room(0), room(1), room(2), room(3), room(4), room(5), room(6), room(8), room(9), room(20));

            stub(snapshots);
            final RoomStats stats = roomStatsService.findStats(30);

            // 0명짜리 방은 어느 칸에도 없다. 참여자 행이 생기기 전에 지워진 방이라 셀 대상이 아니다.
            assertThat(stats.playerBuckets().stream().mapToLong(Bucket::count).sum())
                    .isEqualTo(snapshots.size() - 1);
        }
    }

    @Nested
    class 소요_시간_분포 {

        @Test
        void 끝나지_않은_방은_세지_않는다() {
            // 진행 중인 방을 0분으로 넣으면 "1분 미만"이 진행 중인 방 수만큼 부풀어
            // 방이 빨리 끝나는 것처럼 보인다.
            stub(List.of(new RoomSnapshot(RoomState.PLAYING, BASE, null, 3), lasting(2)));

            final List<Bucket> buckets = roomStatsService.findStats(30).durationBuckets();

            assertThat(buckets.stream().mapToLong(Bucket::count).sum()).isEqualTo(1);
            assertThat(countOf(buckets, "5분 미만")).isEqualTo(1);
        }

        @Test
        void 구간_경계는_아래를_포함하고_위를_뺀다() {
            // 10분짜리 방이 "5~10분"과 "10~20분" 양쪽에 들어가면 합이 방 수보다 커진다.
            stub(List.of(lasting(0), lasting(5), lasting(10), lasting(20), lasting(30), lasting(90)));

            final List<Bucket> buckets = roomStatsService.findStats(30).durationBuckets();

            assertThat(buckets)
                    .extracting(Bucket::label, Bucket::count)
                    .containsExactly(
                            tuple("5분 미만", 1L),
                            tuple("5~10분", 1L),
                            tuple("10~20분", 1L),
                            tuple("20~30분", 1L),
                            tuple("30분 이상", 2L));
        }
    }

    @Nested
    class 상태_분포 {

        @Test
        void 방이_하나도_없는_상태도_0으로_보낸다() {
            // 화면이 빠진 상태를 채우게 두면 상태가 하나 늘었을 때 그 칸이 조용히 사라진다.
            stub(List.of(new RoomSnapshot(RoomState.DONE, BASE, BASE.plusMinutes(2), 3)));

            assertThat(roomStatsService.findStats(30).statuses())
                    .hasSize(RoomState.values().length)
                    .extracting(RoomStats.StatusCount::status)
                    .containsExactly(RoomState.values());
        }

        @Test
        void 상태를_진행_순서대로_준다() {
            stub(List.of());

            assertThat(roomStatsService.findStats(30).statuses().getFirst().status())
                    .isEqualTo(RoomState.READY);
        }
    }

    @Nested
    class 요약_수 {

        @Test
        void 혼자인_방을_칸_이름에_기대지_않고_센다() {
            // 화면이 "1명" 이라는 글자로 칸을 찾아 더하면, 라벨을 한 번 다듬는 순간
            // 숫자가 조용히 0이 된다.
            stub(List.of(room(1), room(1), room(2), room(0)));

            assertThat(roomStatsService.findStats(30).soloRoomCount()).isEqualTo(2);
        }
    }

    @Nested
    class 게임_비중 {

        @Test
        void 시작한_판이_많은_게임부터_주고_비중을_합이_1이_되게_나눈다() {
            given(roomLookupRepository.findSnapshots(any(), any())).willReturn(List.of());
            given(overviewStatisticsRepository.countPlaysByGame(any(), any()))
                    .willReturn(List.of(
                            new GamePlayCount(MiniGameType.RACING_GAME, 1, 1),
                            new GamePlayCount(MiniGameType.CARD_GAME, 3, 2)));
            roomStatsService = new RoomStatsService(roomLookupRepository, overviewStatisticsRepository, CLOCK);

            assertThat(roomStatsService.findStats(30).games())
                    .extracting(stat -> stat.miniGameType().name(), GamePlayStat::started)
                    .containsExactly(tuple("CARD_GAME", 3L), tuple("RACING_GAME", 1L));
            assertThat(roomStatsService.findStats(30).games().getFirst().share())
                    .isEqualTo(0.75);
        }

        @Test
        void 완료_판이_아니라_시작한_판으로_순위를_매긴다() {
            // 완료 판으로 세면 중간에 깨지는 게임이 "아무도 안 고르는 게임"으로 보인다.
            // 둘은 서로 다른 문제라 한 숫자로 뭉개면 안 된다.
            given(roomLookupRepository.findSnapshots(any(), any())).willReturn(List.of());
            given(overviewStatisticsRepository.countPlaysByGame(any(), any()))
                    .willReturn(List.of(
                            new GamePlayCount(MiniGameType.RACING_GAME, 10, 1),
                            new GamePlayCount(MiniGameType.CARD_GAME, 4, 4)));
            roomStatsService = new RoomStatsService(roomLookupRepository, overviewStatisticsRepository, CLOCK);

            assertThat(roomStatsService.findStats(30).games())
                    .extracting(stat -> stat.miniGameType().name(), GamePlayStat::dropped)
                    .containsExactly(tuple("RACING_GAME", 9L), tuple("CARD_GAME", 0L));
        }
    }
}
