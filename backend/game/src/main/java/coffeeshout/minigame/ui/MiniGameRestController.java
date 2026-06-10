package coffeeshout.minigame.ui;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.ui.response.MiniGameRanksResponse;
import coffeeshout.minigame.ui.response.MiniGameScoresResponse;
import coffeeshout.minigame.ui.response.RemainingMiniGameResponse;
import coffeeshout.room.application.service.RoomQueryService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 미니게임 조회 REST 엔드포인트. {@code /rooms/...} 경로 3종은 {@code :room}의
 * {@code RoomRestController}에서 이전했다(ADR-0023 결정 7 — 클라이언트 호환을 위해 URL 유지).
 */
@RestController
@RequiredArgsConstructor
public class MiniGameRestController implements MiniGameApi {

    private final GameSessionService gameSessionService;
    private final RoomQueryService roomQueryService;

    @GetMapping("/minigames/scores")
    public ResponseEntity<MiniGameScoresResponse> getScores(
            @RequestParam String joinCode,
            @RequestParam MiniGameType miniGameType
    ) {
        Map<Gamer, MiniGameScore> result = gameSessionService.getScores(new JoinCode(joinCode), miniGameType);

        return ResponseEntity.ok(MiniGameScoresResponse.from(result));
    }

    @GetMapping("/minigames/ranks")
    public ResponseEntity<MiniGameRanksResponse> getRanks(
            @RequestParam String joinCode,
            @RequestParam MiniGameType miniGameType
    ) {
        final MiniGameResult result = gameSessionService.getRanks(new JoinCode(joinCode), miniGameType);

        return ResponseEntity.ok(MiniGameRanksResponse.from(result));
    }

    @GetMapping("/rooms/minigames")
    public ResponseEntity<List<MiniGameType>> getMiniGames() {
        final List<MiniGameType> responses = Arrays.stream(MiniGameType.values()).toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/rooms/minigames/selected")
    public ResponseEntity<List<MiniGameType>> getSelectedMiniGames(@RequestParam String joinCode) {
        final JoinCode code = validateRoomExists(joinCode);
        final List<MiniGameType> result = gameSessionService.getSelectedTypes(code);

        return ResponseEntity.ok(result);
    }

    // selected와 동일한 대기열 데이터를 응답 래퍼만 달리해 반환한다(이전 :room 엔드포인트의 계약 유지)
    @GetMapping("/rooms/{joinCode}/miniGames/remaining")
    public ResponseEntity<RemainingMiniGameResponse> getRemainingMiniGames(@PathVariable String joinCode) {
        final JoinCode code = validateRoomExists(joinCode);
        final List<MiniGameType> remaining = gameSessionService.getSelectedTypes(code);

        return ResponseEntity.ok(RemainingMiniGameResponse.from(remaining));
    }

    /**
     * 존재하지 않는 방은 404로 거부한다(이전 {@code :room} 엔드포인트의 응답 계약 유지). 세션은 게임 선택
     * 시점에 지연 생성되므로(Step 6의 {@code initSession} 도입 전) 세션 부재만으로는 방 부재를 판별할 수 없다.
     */
    private JoinCode validateRoomExists(String joinCode) {
        final JoinCode code = new JoinCode(joinCode);
        roomQueryService.getByJoinCode(code);
        return code;
    }
}
