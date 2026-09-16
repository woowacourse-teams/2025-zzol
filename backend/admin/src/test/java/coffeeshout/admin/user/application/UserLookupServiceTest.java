package coffeeshout.admin.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import coffeeshout.admin.user.domain.UserLookupRepository;
import coffeeshout.admin.user.domain.UserPlayAggregate;
import coffeeshout.admin.user.domain.UserStats;
import coffeeshout.admin.user.domain.UserStats.Bucket;
import coffeeshout.admin.user.domain.UserStats.DailyCount;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

@DisplayName("UserLookupService.findStats")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserLookupServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** KST 로 2026-09-12 09:00. 자정 경계를 시험하려면 시간대가 붙은 시각이어야 한다. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), KST);

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 12, 9, 0);

    @Mock
    private UserLookupRepository userLookupRepository;

    private UserLookupService userLookupService;

    private void stub(long userCount, List<UserPlayAggregate> plays, List<Instant> signups) {
        given(userLookupRepository.countUsers()).willReturn(userCount);
        given(userLookupRepository.countByProvider()).willReturn(List.of());
        given(userLookupRepository.aggregatePlays()).willReturn(plays);
        given(userLookupRepository.findSignupTimes(any(), any())).willReturn(signups);
        userLookupService = new UserLookupService(userLookupRepository, CLOCK);
    }

    private static UserPlayAggregate played(long id, long plays, long daysAgo) {
        return new UserPlayAggregate(id, plays, NOW.minusDays(daysAgo));
    }

    private static long countOf(List<Bucket> buckets, String label) {
        return buckets.stream()
                .filter(bucket -> bucket.label().equals(label))
                .mapToLong(Bucket::count)
                .sum();
    }

    @Nested
    class 가입_추이 {

        @Test
        void 가입이_없는_날도_0으로_채운다() {
            // 빠진 날을 그대로 두면 recharts 가 이웃한 두 점을 이어, 가입이 없던 날이
            // 그래프에서 사라진다. 실제보다 꾸준해 보인다.
            stub(1, List.of(), List.of(Instant.parse("2026-09-12T00:00:00Z")));

            final List<DailyCount> signups = userLookupService.findStats(7).signups();

            assertThat(signups).hasSize(7);
            assertThat(signups.getFirst().date()).isEqualTo(LocalDate.of(2026, 9, 6));
            assertThat(signups.getLast().date()).isEqualTo(LocalDate.of(2026, 9, 12));
            assertThat(signups.stream().mapToLong(DailyCount::count).sum()).isEqualTo(1);
        }

        @Test
        void 날짜는_UTC_가_아니라_운영자의_시간대로_묶는다() {
            // 2026-09-11T20:00Z 는 KST 로 9월 12일 새벽 5시다. UTC 로 묶으면 11일 막대에
            // 꽂혀, 자정 근처 가입이 하루 옆으로 밀린다.
            stub(1, List.of(), List.of(Instant.parse("2026-09-11T20:00:00Z")));

            final List<DailyCount> signups = userLookupService.findStats(7).signups();

            assertThat(signups.getLast().count()).isEqualTo(1);
        }
    }

    @Nested
    class 참여도_분포 {

        @Test
        void 한_번만_해_본_사람을_따로_센다() {
            stub(
                    6,
                    List.of(played(1, 1, 0), played(2, 2, 0), played(3, 4, 0), played(4, 7, 0), played(5, 30, 0)),
                    List.of());

            assertThat(userLookupService.findStats(7).playBuckets())
                    .extracting(Bucket::label, Bucket::count)
                    .containsExactly(
                            tuple("0회", 1L),
                            tuple("1회", 1L),
                            tuple("2~4회", 2L),
                            tuple("5~9회", 1L),
                            tuple("10회 이상", 1L));
        }

        @Test
        void 한_판도_안_한_회원이_0회_칸에_들어간다() {
            // 방에만 들어온 사람은 aggregatePlays 에 판 수 0으로 온다. 아예 방에 안 들어온
            // 사람은 목록에 없다. 둘 다 0회 칸에 들어가야 칸의 합이 회원 수가 된다.
            stub(5, List.of(played(1, 0, 0), played(2, 3, 0)), List.of());

            final List<Bucket> buckets = userLookupService.findStats(7).playBuckets();

            assertThat(countOf(buckets, "0회")).isEqualTo(4);
            assertThat(buckets.stream().mapToLong(Bucket::count).sum()).isEqualTo(5);
        }
    }

    @Nested
    class 요약_수 {

        @Test
        void 한_판_이상과_최근_7일을_칸_이름에_기대지_않고_센다() {
            // 화면이 "0회", "최근 7일" 이라는 글자로 칸을 찾아 더하면, 라벨을 한 번 다듬는
            // 순간 숫자가 조용히 0이 된다. 서버가 세어 따로 내려보낸다.
            stub(5, List.of(played(1, 0, 0), played(2, 1, 3), played(3, 2, 20)), List.of());

            final UserStats stats = userLookupService.findStats(7);

            assertThat(stats.playedUserCount()).isEqualTo(2);
            assertThat(stats.activeUserCount()).isEqualTo(2);
        }

        @Test
        void 마지막_참여가_7일_전이면_최근_7일에_들어가지_않는다() {
            stub(1, List.of(played(1, 1, 7)), List.of());

            assertThat(userLookupService.findStats(7).activeUserCount()).isZero();
        }
    }

    @Nested
    class 잔존_분포 {

        @Test
        void 마지막_참여로부터_지난_날로_나눈다() {
            stub(5, List.of(played(1, 1, 0), played(2, 1, 20), played(3, 1, 60), played(4, 1, 200)), List.of());

            assertThat(userLookupService.findStats(7).activityBuckets())
                    .extracting(Bucket::label, Bucket::count)
                    .containsExactly(
                            tuple("최근 7일", 1L),
                            tuple("8~30일", 1L),
                            tuple("31~90일", 1L),
                            tuple("90일 초과", 1L),
                            tuple("참여 없음", 1L));
        }

        @Test
        void 구간_경계는_아래를_포함하고_위를_뺀다() {
            // 7일 전 참여가 "최근 7일"과 "8~30일" 양쪽에 들어가면 칸의 합이 회원 수보다 커진다.
            stub(2, List.of(played(1, 1, 7), played(2, 1, 30)), List.of());

            final List<Bucket> buckets = userLookupService.findStats(7).activityBuckets();

            assertThat(countOf(buckets, "최근 7일")).isZero();
            assertThat(countOf(buckets, "8~30일")).isEqualTo(1);
            assertThat(countOf(buckets, "31~90일")).isEqualTo(1);
            assertThat(buckets.stream().mapToLong(Bucket::count).sum()).isEqualTo(2);
        }

        @Test
        void 참여도와_잔존은_기간에_걸리지_않는다() {
            // 기간을 걸면 그 기간에 활동한 사람만 남아, 빠져나간 사람을 묻는 그래프에서
            // 빠져나간 사람이 사라진다. 200일 전 참여자가 7일짜리 조회에도 보여야 한다.
            stub(1, List.of(played(1, 1, 200)), List.of());

            final UserStats stats = userLookupService.findStats(7);

            assertThat(countOf(stats.activityBuckets(), "90일 초과")).isEqualTo(1);
            assertThat(countOf(stats.playBuckets(), "1회")).isEqualTo(1);
        }
    }
}
