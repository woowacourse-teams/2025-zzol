package coffeeshout.admin.user.application;

import coffeeshout.admin.user.domain.ProviderCount;
import coffeeshout.admin.user.domain.UserActivity;
import coffeeshout.admin.user.domain.UserListRow;
import coffeeshout.admin.user.domain.UserLookupRepository;
import coffeeshout.admin.user.domain.UserPlayAggregate;
import coffeeshout.admin.user.domain.UserStats;
import coffeeshout.admin.user.domain.UserStats.Bucket;
import coffeeshout.admin.user.domain.UserStats.DailyCount;
import coffeeshout.admin.user.domain.UserSummary;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLookupService {

    private static final int PAGE_SIZE = 20;

    /**
     * 플레이 수 구간. 경계를 1, 2, 5, 10 으로 잡았다.
     *
     * <p>한 번만 해 보고 안 돌아온 사람(1회)을 첫 칸에 혼자 세운다. 이 칸이 크면 첫 판에서
     * 뭔가 잃고 있다는 뜻이고, 그건 2~4회와 섞어 놓으면 안 보인다.
     */
    private static final List<PlayBand> PLAY_BANDS = List.of(
            new PlayBand("1회", 1, 1),
            new PlayBand("2~4회", 2, 4),
            new PlayBand("5~9회", 5, 9),
            new PlayBand("10회 이상", 10, Long.MAX_VALUE));

    /** 마지막 플레이로부터 지난 날. 7일과 30일은 주간, 월간 활동자의 관행적 경계다. */
    private static final List<ActivityBand> ACTIVITY_BANDS = List.of(
            new ActivityBand("최근 7일", 7),
            new ActivityBand("8~30일", 30),
            new ActivityBand("31~90일", 90),
            new ActivityBand("90일 초과", Long.MAX_VALUE));

    private static final String NEVER_PLAYED = "참여 없음";

    private final UserLookupRepository userLookupRepository;
    private final Clock clock;

    public Page<UserListRow> search(String keyword, int page) {
        return userLookupRepository.search(keyword, PageRequest.of(page, PAGE_SIZE));
    }

    /** 소셜 제공자 분포. 활성 회원 수를 함께 준다 - 연결 수의 합과 회원 수는 다르다. */
    public ProviderStats findProviderStats() {
        return new ProviderStats(userLookupRepository.countUsers(), userLookupRepository.countByProvider());
    }

    /**
     * 유저 화면 상단 그래프.
     *
     * <p>가입 추이만 기간을 받는다. 나머지 둘은 <b>지금 회원 전체</b>의 분포다. 참여도와
     * 잔존을 최근 30일로 자르면 그 기간에 활동한 사람만 남아, "얼마나 빠져나갔나"를 묻는
     * 그래프에서 빠져나간 사람이 사라진다.
     */
    public UserStats findStats(int days) {
        final ZoneId zone = clock.getZone();
        final LocalDate today = LocalDate.now(clock);
        final LocalDate first = today.minusDays(days - 1L);
        final Instant from = first.atStartOfDay(zone).toInstant();
        final Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();

        final List<UserPlayAggregate> plays = userLookupRepository.aggregatePlays();
        final long userCount = userLookupRepository.countUsers();
        final LocalDateTime now = LocalDateTime.now(clock);
        return new UserStats(
                userCount,
                userLookupRepository.countByProvider(),
                toDailySignups(userLookupRepository.findSignupTimes(from, to), zone, first, today),
                toPlayBuckets(plays, userCount),
                toActivityBuckets(plays, userCount, now),
                plays.stream().filter(play -> play.playCount() > 0).count(),
                countActive(plays, now));
    }

    /**
     * 가입이 없는 날도 0으로 채운다.
     *
     * <p>빠진 날을 그대로 두면 recharts 가 이웃한 두 점을 이어 버려 <b>가입이 없던 날이
     * 그래프에서 사라진다</b>. 30일 중 5일만 가입이 있으면 다섯 점짜리 그래프가 되어
     * 실제보다 꾸준해 보인다.
     */
    private static List<DailyCount> toDailySignups(List<Instant> times, ZoneId zone, LocalDate first, LocalDate last) {
        final Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            counts.put(date, 0L);
        }
        for (Instant time : times) {
            counts.computeIfPresent(LocalDate.ofInstant(time, zone), (date, count) -> count + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new DailyCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** 최근 7일 안에 방에 들어온 회원. 주간 활동 회원이라 부르는 값이다. */
    private static long countActive(List<UserPlayAggregate> plays, LocalDateTime now) {
        return plays.stream()
                .filter(play -> play.lastPlayedAt() != null)
                .filter(play -> Duration.between(play.lastPlayedAt(), now).toDays() < UserStats.ACTIVE_DAYS)
                .count();
    }

    private static List<Bucket> toPlayBuckets(List<UserPlayAggregate> plays, long userCount) {
        final List<Bucket> buckets = new ArrayList<>();
        long counted = 0;
        for (PlayBand band : PLAY_BANDS) {
            final long count = plays.stream()
                    .filter(play -> band.contains(play.playCount()))
                    .count();
            counted += count;
            buckets.add(new Bucket(band.label(), count));
        }
        // 남는 사람은 한 판도 안 끝낸 회원이다. 방에 들어오기만 한 사람도 여기 들어간다.
        buckets.add(0, new Bucket("0회", Math.max(0, userCount - counted)));
        return buckets;
    }

    private static List<Bucket> toActivityBuckets(List<UserPlayAggregate> plays, long userCount, LocalDateTime now) {
        final List<Bucket> buckets = new ArrayList<>();
        long previousBound = 0;
        long counted = 0;
        for (ActivityBand band : ACTIVITY_BANDS) {
            final long lower = previousBound;
            final long count = plays.stream()
                    .filter(play -> play.lastPlayedAt() != null)
                    .filter(play -> {
                        final long elapsed =
                                Duration.between(play.lastPlayedAt(), now).toDays();
                        return elapsed >= lower && elapsed < band.withinDays();
                    })
                    .count();
            previousBound = band.withinDays();
            counted += count;
            buckets.add(new Bucket(band.label(), count));
        }
        buckets.add(new Bucket(NEVER_PLAYED, Math.max(0, userCount - counted)));
        return buckets;
    }

    private record PlayBand(String label, long min, long max) {
        boolean contains(long plays) {
            return plays >= min && plays <= max;
        }
    }

    private record ActivityBand(String label, long withinDays) {}

    public UserDetail findDetail(Long userId) {
        final UserSummary summary = userLookupRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_EXIST, "유저를 찾을 수 없습니다: " + userId));

        return new UserDetail(
                summary, userLookupRepository.findProviders(userId), userLookupRepository.findActivity(userId));
    }

    public record UserDetail(UserSummary summary, List<String> providers, UserActivity activity) {}

    public record ProviderStats(long userCount, List<ProviderCount> counts) {}
}
