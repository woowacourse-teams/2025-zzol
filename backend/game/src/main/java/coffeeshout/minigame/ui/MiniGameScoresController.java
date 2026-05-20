package coffeeshout.minigame.ui;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.ui.response.MiniGameRanksResponse;
import coffeeshout.minigame.ui.response.MiniGameScoresResponse;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/minigames")
@RequiredArgsConstructor
public class MiniGameScoresController {

    private final GameSessionService gameSessionService;

    @GetMapping("/scores")
    public ResponseEntity<MiniGameScoresResponse> getScores(
            @RequestParam String joinCode,
            @RequestParam MiniGameType miniGameType
    ) {
        final Map<PlayerName, MiniGameScore> scores = gameSessionService.getScores(
                new JoinCode(joinCode), miniGameType);
        return ResponseEntity.ok(MiniGameScoresResponse.from(scores));
    }

    @GetMapping("/ranks")
    public ResponseEntity<MiniGameRanksResponse> getRanks(
            @RequestParam String joinCode,
            @RequestParam MiniGameType miniGameType
    ) {
        final MiniGameResult result = gameSessionService.getRanks(
                new JoinCode(joinCode), miniGameType);
        return ResponseEntity.ok(MiniGameRanksResponse.from(result));
    }
}
