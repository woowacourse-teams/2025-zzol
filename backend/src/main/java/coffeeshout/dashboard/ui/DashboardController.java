package coffeeshout.dashboard.ui;

import coffeeshout.dashboard.application.DashboardService;
import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController implements DashboardApi {

    private final DashboardService dashboardService;

    @GetMapping("/top-winners")
    public ResponseEntity<List<TopWinnerResponse>> getTop5Winners() {
        return ResponseEntity.ok(dashboardService.getTop5Winners());
    }

    @GetMapping("/lowest-probability-winner")
    public ResponseEntity<LowestProbabilityWinnerResponse> getLowestProbabilityWinner() {
        return ResponseEntity.ok(dashboardService.getLowestProbabilityWinner());
    }

    @GetMapping("/game-play-counts")
    public ResponseEntity<List<GamePlayCountResponse>> getGamePlayCounts() {
        return ResponseEntity.ok(dashboardService.getGamePlayCounts());
    }
}
