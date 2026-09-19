package coffeeshout.admin.overview.ui.response;

import coffeeshout.admin.overview.application.OverviewService.DailySummary;
import java.time.LocalDate;

public record DailySummaryResponse(LocalDate date, FunnelResponse funnel, long players, long signups) {

    public static DailySummaryResponse from(DailySummary summary) {
        return new DailySummaryResponse(
                summary.date(), FunnelResponse.from(summary.funnel()), summary.players(), summary.signups());
    }
}
