package coffeeshout.admin.audit.application;

import coffeeshout.admin.audit.domain.AdminAuditLog;
import coffeeshout.admin.audit.domain.AdminAuditLogRepository;
import coffeeshout.admin.audit.domain.AdminAuditResult;
import coffeeshout.admin.audit.domain.AdminAuditStats;
import coffeeshout.admin.audit.domain.AdminAuditStats.ActionCount;
import coffeeshout.admin.audit.domain.AdminAuditStats.ActorCount;
import coffeeshout.admin.audit.domain.AdminAuditStats.DailyCount;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 조치 이력 화면 상단 그래프.
 *
 * <p>{@link AdminAuditLogService} 와 나눴다. 저쪽은 <b>쓰는</b> 쪽이고 AOP 가 부른다.
 * 읽는 코드를 거기 두면 모든 관리자 조치가 통계 코드를 함께 로드하게 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuditStatsService {

    /**
     * 담당자 막대는 다섯까지만 세운다.
     *
     * <p>관리자가 늘면 가로축에 이메일이 줄줄이 서서 글자가 겹친다. 여섯 번째부터는 하나로
     * 묶는다. 개별 담당자를 봐야 하면 표의 담당자 필터로 가는 것이 맞다.
     */
    private static final int ACTOR_LIMIT = 5;

    private static final String OTHER_ACTORS = "그 외";

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final Clock clock;

    public AdminAuditStats findStats(int days) {
        final ZoneId zone = clock.getZone();
        final LocalDate today = LocalDate.now(clock);
        final LocalDate first = today.minusDays(days - 1L);
        final List<AdminAuditLog> logs = adminAuditLogRepository.findAllByCreatedAtBetween(
                first.atStartOfDay(zone).toInstant(),
                today.plusDays(1).atStartOfDay(zone).toInstant());

        return new AdminAuditStats(
                logs.size(),
                logs.stream()
                        .filter(log -> log.getResult() == AdminAuditResult.FAILURE)
                        .count(),
                countActions(logs),
                countActors(logs),
                countByDate(logs, zone, first, today));
    }

    private static List<ActionCount> countActions(List<AdminAuditLog> logs) {
        final Map<String, Long> byAction = new LinkedHashMap<>();
        for (AdminAuditLog log : logs) {
            byAction.merge(log.getAction(), 1L, Long::sum);
        }
        return byAction.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new ActionCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 많이 한 다섯 명과 나머지 한 칸.
     *
     * <p>나머지를 <b>버리지 않고 묶는</b> 이유는, 막대의 합이 전체 조치 수와 맞아야
     * "이 다섯이 거의 다 했다"와 "다섯은 일부일 뿐이다"가 구분되기 때문이다.
     */
    private static List<ActorCount> countActors(List<AdminAuditLog> logs) {
        final Map<String, Long> byActor = new LinkedHashMap<>();
        for (AdminAuditLog log : logs) {
            byActor.merge(log.getActorEmail(), 1L, Long::sum);
        }

        final List<ActorCount> sorted = byActor.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new ActorCount(entry.getKey(), entry.getValue()))
                .toList();
        if (sorted.size() <= ACTOR_LIMIT) {
            return sorted;
        }

        final long rest =
                sorted.stream().skip(ACTOR_LIMIT).mapToLong(ActorCount::count).sum();
        return java.util.stream.Stream.concat(
                        sorted.stream().limit(ACTOR_LIMIT),
                        java.util.stream.Stream.of(new ActorCount(OTHER_ACTORS, rest)))
                .toList();
    }

    private static List<DailyCount> countByDate(
            List<AdminAuditLog> logs, ZoneId zone, LocalDate first, LocalDate last) {
        final Map<LocalDate, long[]> counts = new LinkedHashMap<>();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            counts.put(date, new long[2]);
        }
        for (AdminAuditLog log : logs) {
            final long[] day = counts.get(LocalDate.ofInstant(log.getCreatedAt(), zone));
            if (day != null) {
                day[log.getResult() == AdminAuditResult.FAILURE ? 1 : 0]++;
            }
        }
        return counts.entrySet().stream()
                .map(entry -> new DailyCount(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }
}
