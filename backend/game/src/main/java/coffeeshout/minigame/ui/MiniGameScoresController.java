package coffeeshout.minigame.ui;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.ui.response.MiniGameRanksResponse;
import coffeeshout.minigame.ui.response.MiniGameScoresResponse;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.user.application.service.UserProfileService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final UserProfileService userProfileService;

    @GetMapping("/scores")
    public ResponseEntity<MiniGameScoresResponse> getScores(
            @RequestParam String joinCode,
            @RequestParam MiniGameType miniGameType
    ) {
        final Map<Gamer, MiniGameScore> scores = gameSessionService.getScores(
                new JoinCode(joinCode), miniGameType);
        final Map<Long, String> userCodes = resolveUserCodes(scores.keySet());
        return ResponseEntity.ok(MiniGameScoresResponse.from(scores, userCodes));
    }

    @GetMapping("/ranks")
    public ResponseEntity<MiniGameRanksResponse> getRanks(
            @RequestParam String joinCode,
            @RequestParam MiniGameType miniGameType
    ) {
        final MiniGameResult result = gameSessionService.getRanks(
                new JoinCode(joinCode), miniGameType);
        final Map<Long, String> userCodes = resolveUserCodes(result.getRank().keySet());
        return ResponseEntity.ok(MiniGameRanksResponse.from(result, userCodes));
    }

    private Map<Long, String> resolveUserCodes(Collection<Gamer> gamers) {
        final List<Long> userIds = gamers.stream()
                .map(Gamer::userId)
                .filter(Objects::nonNull)
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileService.findUserCodesByIds(userIds);
    }
}
