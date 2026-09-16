package coffeeshout.admin.overview.ui;

import coffeeshout.admin.overview.application.OverviewService;
import coffeeshout.admin.overview.ui.response.ActionQueueResponse;
import coffeeshout.admin.overview.ui.response.DailySummaryResponse;
import coffeeshout.admin.overview.ui.response.DailyTrendResponse;
import coffeeshout.admin.overview.ui.response.GamePlayStatResponse;
import coffeeshout.admin.overview.ui.response.PeriodSummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 홈 대시보드. 화면 상단부터 순서대로 대응한다.
 *
 * <p>큐와 요약을 한 응답으로 묶지 않았다. 큐는 조치가 있을 때마다 다시 불러야 하고
 * 요약은 하루에 몇 번 안 바뀐다. 갱신 주기가 다른 것을 한 응답에 묶으면
 * 화면이 필요 없는 집계 쿼리를 계속 돌리게 된다.
 */
@RestController
@RequestMapping("/admin/api/overview")
@Validated
@RequiredArgsConstructor
public class AdminOverviewController {

    private final OverviewService overviewService;

    @GetMapping("/action-queue")
    public ActionQueueResponse actionQueue() {
        return ActionQueueResponse.from(overviewService.actionQueue());
    }

    /**
     * 최근 흐름. 오늘 숫자만 보여주면 "0인데 정상인가"에 답할 수 없다.
     *
     * @param days 상한을 둔다. 기간을 늘려 전체 스캔을 유발하는 것을 막는다.
     */
    @GetMapping("/trend")
    public List<DailyTrendResponse> trend(@RequestParam(defaultValue = "14") @Min(2) @Max(90) int days) {
        return overviewService.trend(days).stream()
                .map(DailyTrendResponse::from)
                .toList();
    }

    /** 게임별 완료 수와 비중. 비중이 0에 가까운 게임은 아무도 고르지 않는다는 뜻이다. */
    @GetMapping("/games")
    public List<GamePlayStatResponse> games(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return overviewService.gamePlayStats(days).stream()
                .map(GamePlayStatResponse::from)
                .toList();
    }

    /**
     * 기간 합계. 분석 화면이 쓴다.
     *
     * <p>{@code /summary} 를 N번 부르는 것으로 대신할 수 없다. 어제 생성돼 오늘 끝난 방은
     * 하루치를 더하면 생성과 완주가 다른 날로 갈려 퍼널이 맞지 않는다.
     */
    @GetMapping("/period")
    public PeriodSummaryResponse period(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return PeriodSummaryResponse.from(overviewService.periodSummary(days));
    }

    /** 날짜를 안 주면 오늘(KST)이다. */
    @GetMapping("/summary")
    public DailySummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return DailySummaryResponse.from(date == null ? overviewService.today() : overviewService.summaryOf(date));
    }
}
