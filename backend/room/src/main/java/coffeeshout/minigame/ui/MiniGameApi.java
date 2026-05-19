package coffeeshout.minigame.ui;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.ui.response.MiniGameRanksResponse;
import coffeeshout.minigame.ui.response.MiniGameScoresResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "MiniGame", description = "미니게임 관련 API")
public interface MiniGameApi {

    @Operation(summary = "미니게임 점수 조회", description = "특정 방의 미니게임 점수를 조회합니다.")
    ResponseEntity<MiniGameScoresResponse> getScores(
            @Parameter(description = "방 입장 코드", required = true) String joinCode,
            @Parameter(description = "미니게임 타입", required = true) MiniGameType miniGameType
    );

    @Operation(summary = "미니게임 순위 조회", description = "특정 방의 미니게임 순위를 조회합니다.")
    ResponseEntity<MiniGameRanksResponse> getRanks(
            @Parameter(description = "방 입장 코드", required = true) String joinCode,
            @Parameter(description = "미니게임 타입", required = true) MiniGameType miniGameType
    );
}
