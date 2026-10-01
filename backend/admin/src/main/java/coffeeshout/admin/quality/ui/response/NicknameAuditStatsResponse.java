package coffeeshout.admin.quality.ui.response;

import coffeeshout.admin.quality.domain.NicknameAuditStats;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.time.LocalDate;
import java.util.List;

/** 닉네임 검열 화면 상단 그래프. */
public record NicknameAuditStatsResponse(
        long total, List<StatusSlice> statuses, List<DailyPoint> daily, List<BucketSlice> confidenceBuckets) {

    public static NicknameAuditStatsResponse from(NicknameAuditStats stats) {
        return new NicknameAuditStatsResponse(
                stats.total(),
                stats.statuses().stream()
                        .map(row -> new StatusSlice(row.status(), row.count()))
                        .toList(),
                stats.daily().stream()
                        .map(row -> new DailyPoint(row.date(), row.flagged(), row.passed()))
                        .toList(),
                stats.confidenceBuckets().stream()
                        .map(row -> new BucketSlice(row.label(), row.count()))
                        .toList());
    }

    public record StatusSlice(NicknameAuditStatus status, long count) {}

    public record DailyPoint(LocalDate date, long flagged, long passed) {}

    public record BucketSlice(String label, long count) {}
}
