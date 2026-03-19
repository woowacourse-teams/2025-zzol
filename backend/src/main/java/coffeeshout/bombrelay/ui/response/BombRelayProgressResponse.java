package coffeeshout.bombrelay.ui.response;

import coffeeshout.bombrelay.domain.event.BombRelayProgressEvent;
import coffeeshout.bombrelay.domain.event.BombRelayProgressEvent.PlayerProgress;
import java.util.List;

public record BombRelayProgressResponse(
        String currentWord,
        String currentTurnPlayerName,
        int currentRound,
        List<PlayerProgress> players
) {

    public static BombRelayProgressResponse from(BombRelayProgressEvent event) {
        return new BombRelayProgressResponse(
                event.currentWord(),
                event.currentTurnPlayerName(),
                event.currentRound(),
                event.players()
        );
    }
}
