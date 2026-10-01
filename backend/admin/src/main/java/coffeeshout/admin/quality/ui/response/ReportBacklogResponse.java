package coffeeshout.admin.quality.ui.response;

import coffeeshout.admin.quality.domain.ReportBacklog;

/**
 * 신고가 지금 얼마나 밀렸는가.
 *
 * @param pendingCount         아직 처리하지 않은 신고
 * @param oldestPendingMinutes 가장 오래 기다린 미처리 신고의 나이. "지금 밀렸나"에 답한다
 */
public record ReportBacklogResponse(long pendingCount, long oldestPendingMinutes) {

    public static ReportBacklogResponse from(ReportBacklog sla) {
        return new ReportBacklogResponse(sla.pendingCount(), sla.oldestPendingMinutes());
    }
}
