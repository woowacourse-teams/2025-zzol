package coffeeshout.bombrelay.ui.response;

import coffeeshout.bombrelay.domain.event.BombRelayStateChangedEvent;

public record BombRelayStateResponse(
        String state,
        int currentRound,
        int maxRounds,
        String currentWord,
        String currentTurnPlayerName
) {

    public static BombRelayStateResponse from(BombRelayStateChangedEvent event) {
        return new BombRelayStateResponse(
                event.state(),
                event.currentRound(),
                event.maxRounds(),
                event.currentWord(),
                event.currentTurnPlayerName()
        );
    }
}
