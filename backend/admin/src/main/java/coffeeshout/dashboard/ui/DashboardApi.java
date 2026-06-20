package coffeeshout.dashboard.ui;

import coffeeshout.dashboard.domain.BlindTimerTopPlayerResponse;
import coffeeshout.dashboard.domain.BlockStackingTopPlayerResponse;
import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.SpeedTouchTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Dashboard", description = "대시보드 통계 API")
public interface DashboardApi {

    @Operation(summary = "상위 당첨자 조회", description = "이번 달 가장 많이 당첨된 상위 5명의 당첨자를 조회합니다.")
    ResponseEntity<List<TopWinnerResponse>> getTop5Winners();

    @Operation(summary = "최저 확률 당첨자 조회", description = "이번 달 가장 낮은 확률로 당첨된 닉네임을 조회합니다.")
    ResponseEntity<LowestProbabilityWinnerResponse> getLowestProbabilityWinner();

    @Operation(summary = "게임 실행 횟수 조회", description = "이번 달 제일 많이 실행된 게임 통계를 조회합니다.")
    ResponseEntity<List<GamePlayCountResponse>> getGamePlayCounts();

    @Operation(summary = "레이싱 게임 TOP 플레이어 조회", description = "이번 달 레이싱 게임 평균 순위 기준 상위 5명을 조회합니다.")
    ResponseEntity<List<RacingGameTopPlayerResponse>> getRacingGameTopPlayers();

    @Operation(summary = "블록 쌓기 TOP 플레이어 조회", description = "이번 달 블록 쌓기 게임 최고 층수 기준 상위 5명을 조회합니다.")
    ResponseEntity<List<BlockStackingTopPlayerResponse>> getBlockStackingTopPlayers();

    @Operation(summary = "스피드터치 TOP 플레이어 조회", description = "이번 달 스피드터치(1to25) 최단 완주 시간 기준 상위 5명을 조회합니다.")
    ResponseEntity<List<SpeedTouchTopPlayerResponse>> getSpeedTouchTopPlayers();

    @Operation(summary = "뇌피셜 초시계 TOP 플레이어 조회", description = "이번 달 블라인드타이머 목표 시간과의 오차가 가장 작은 상위 5명을 조회합니다.")
    ResponseEntity<List<BlindTimerTopPlayerResponse>> getBlindTimerTopPlayers();
}
