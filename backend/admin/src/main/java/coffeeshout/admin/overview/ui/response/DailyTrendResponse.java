package coffeeshout.admin.overview.ui.response;

import coffeeshout.admin.overview.domain.DailyTrendPoint;
import java.time.LocalDate;

public record DailyTrendResponse(LocalDate date, long created, long completed, long players) {

    public static DailyTrendResponse from(DailyTrendPoint point) {
        return new DailyTrendResponse(point.date(), point.created(), point.completed(), point.players());
    }
}
