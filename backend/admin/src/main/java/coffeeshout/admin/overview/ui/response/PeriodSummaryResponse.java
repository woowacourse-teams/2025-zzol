package coffeeshout.admin.overview.ui.response;

import coffeeshout.admin.overview.application.OverviewService.PeriodSummary;
import java.time.LocalDate;

/**
 * 기간 합계. {@link DailySummaryResponse}와 모양이 거의 같지만 합치지 않았다.
 *
 * <p>홈의 "오늘"은 날짜 하나를 말하고 분석 화면의 "최근 30일"은 구간을 말한다.
 * 한 응답에 {@code date}와 {@code from}/{@code to}를 다 담고 상황에 따라 한쪽을 비우면,
 * 쓰는 쪽이 매번 어느 쪽이 채워졌는지 따져야 한다.
 *
 * @param days          요청한 일수. 화면이 되묻지 않고 그대로 라벨에 쓴다
 * @param avgPlayersPerRoom 방당 평균 참여자. 방 수가 0이면 0
 */
public record PeriodSummaryResponse(
        int days,
        LocalDate from,
        LocalDate to,
        FunnelResponse funnel,
        long players,
        long signups,
        double avgPlayersPerRoom) {

    public static PeriodSummaryResponse from(PeriodSummary summary) {
        return new PeriodSummaryResponse(
                summary.days(),
                summary.from(),
                summary.to(),
                FunnelResponse.from(summary.funnel()),
                summary.players(),
                summary.signups(),
                summary.avgPlayersPerRoom());
    }
}
