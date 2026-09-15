package coffeeshout.admin.audit.ui.response;

import coffeeshout.admin.audit.domain.AdminAuditStats;
import java.time.LocalDate;
import java.util.List;

/**
 * 조치 이력 화면 상단 그래프.
 *
 * <p>{@code action} 은 매핑 패턴 그대로다. 한글 이름은 화면이 붙인다. 패턴은 컨트롤러
 * 경로에서 나오므로 새 조치가 생기면 서버가 모르는 사이에 값이 늘고, 그때 화면은 원문을
 * 그대로 찍어 "모르는 조치가 생겼다"는 사실을 보이게 한다.
 */
public record AdminAuditStatsResponse(
        long total, long failed, List<ActionSlice> actions, List<ActorSlice> actors, List<DailyPoint> daily) {

    public static AdminAuditStatsResponse from(AdminAuditStats stats) {
        return new AdminAuditStatsResponse(
                stats.total(),
                stats.failed(),
                stats.actions().stream()
                        .map(row -> new ActionSlice(row.action(), row.count()))
                        .toList(),
                stats.actors().stream()
                        .map(row -> new ActorSlice(row.actorEmail(), row.count()))
                        .toList(),
                stats.daily().stream()
                        .map(row -> new DailyPoint(row.date(), row.success(), row.failure()))
                        .toList());
    }

    public record ActionSlice(String action, long count) {}

    public record ActorSlice(String actorEmail, long count) {}

    public record DailyPoint(LocalDate date, long success, long failure) {}
}
