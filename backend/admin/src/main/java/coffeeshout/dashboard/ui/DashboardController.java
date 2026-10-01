package coffeeshout.dashboard.ui;

import coffeeshout.dashboard.application.DashboardService;
import coffeeshout.dashboard.domain.BlindTimerTopPlayerResponse;
import coffeeshout.dashboard.domain.BlockStackingTopPlayerResponse;
import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.SpeedTouchTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.WormGameTopPlayerResponse;
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

    @Override
    @GetMapping("/top-winners")
    public ResponseEntity<List<TopWinnerResponse>> getTop5Winners() {
        return ResponseEntity.ok(dashboardService.getTop5Winners());
    }

    @Override
    @GetMapping("/lowest-probability-winner")
    public ResponseEntity<LowestProbabilityWinnerResponse> getLowestProbabilityWinner() {
        return ResponseEntity.ok(dashboardService.getLowestProbabilityWinner());
    }

    @Override
    @GetMapping("/game-play-counts")
    public ResponseEntity<List<GamePlayCountResponse>> getGamePlayCounts() {
        return ResponseEntity.ok(dashboardService.getGamePlayCounts());
    }

    @Override
    @GetMapping("/racing-game-top-players")
    public ResponseEntity<List<RacingGameTopPlayerResponse>> getRacingGameTopPlayers() {
        return ResponseEntity.ok(dashboardService.getRacingGameTopPlayers());
    }

    @Override
    @GetMapping("/block-stacking-top-players")
    public ResponseEntity<List<BlockStackingTopPlayerResponse>> getBlockStackingTopPlayers() {
        return ResponseEntity.ok(dashboardService.getBlockStackingTopPlayers());
    }

    @Override
    @GetMapping("/worm-game-top-players")
    public ResponseEntity<List<WormGameTopPlayerResponse>> getWormGameTopPlayers() {
        return ResponseEntity.ok(dashboardService.getWormGameTopPlayers());
    }

    @Override
    @GetMapping("/speed-touch-top-players")
    public ResponseEntity<List<SpeedTouchTopPlayerResponse>> getSpeedTouchTopPlayers() {
        return ResponseEntity.ok(dashboardService.getSpeedTouchTopPlayers());
    }

    @Override
    @GetMapping("/blind-timer-top-players")
    public ResponseEntity<List<BlindTimerTopPlayerResponse>> getBlindTimerTopPlayers() {
        return ResponseEntity.ok(dashboardService.getBlindTimerTopPlayers());
    }
}
