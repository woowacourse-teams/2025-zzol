package coffeeshout.minigame.ui;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.ui.response.RemainingMiniGameResponse;
import coffeeshout.room.domain.JoinCode;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class MiniGameRestController {

    private final GameSessionService gameSessionService;

    @GetMapping("/minigames")
    public ResponseEntity<List<MiniGameType>> getMiniGames() {
        return ResponseEntity.ok(Arrays.stream(MiniGameType.values()).toList());
    }

    @GetMapping("/minigames/selected")
    public ResponseEntity<List<MiniGameType>> getSelectedMiniGames(@RequestParam String joinCode) {
        final GameSession session = gameSessionService.getSession(new JoinCode(joinCode));
        return ResponseEntity.ok(session.getSelectedTypes());
    }

    @GetMapping("/{joinCode}/miniGames/remaining")
    public ResponseEntity<RemainingMiniGameResponse> getRemainingMiniGames(@PathVariable String joinCode) {
        final GameSession session = gameSessionService.getSession(new JoinCode(joinCode));
        final List<String> names = session.getPendingGamesView().stream()
                .map(g -> g.getMiniGameType().name())
                .toList();
        return ResponseEntity.ok(RemainingMiniGameResponse.of(names));
    }
}
