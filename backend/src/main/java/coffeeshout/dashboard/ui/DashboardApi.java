package coffeeshout.dashboard.ui;

import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
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
}
