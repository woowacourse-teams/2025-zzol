package coffeeshout.minigame.ui;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.ui.response.MiniGameRanksResponse;
import coffeeshout.minigame.ui.response.MiniGameScoresResponse;
import coffeeshout.minigame.ui.response.RemainingMiniGameResponse;
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
 * {@code RoomRestController}에서 이전했다(ADR-0025 결정 7 — 클라이언트 호환을 위해 URL 유지).
 */
@RestController
@RequiredArgsConstructor
public class MiniGameRestController implements MiniGameApi {

    private final GameSessionService gameSessionService;

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
     * 존재하지 않는 방은 404로 거부한다(이전 {@code :room} 엔드포인트의 응답 계약 유지). GameSession은 방 생성
     * 시점에 {@code GameSessionInitConsumer}가 사전 생성하고 방 삭제 시 정리하므로 방과 1:1 대응한다
     * (ADR-0025 결정 6 — 지연 생성 폐지). 따라서 세션 부재를 방 부재로 판정할 수 있어 {@code :room} 조회 없이
     * 동일한 404 계약을 유지한다.
     */
    private JoinCode validateRoomExists(String joinCode) {
        final JoinCode code = new JoinCode(joinCode);
        if (gameSessionService.findSession(code).isEmpty()) {
            throw new BusinessException(GlobalErrorCode.NOT_EXIST, "방이 존재하지 않습니다.");
        }
        return code;
    }
}
