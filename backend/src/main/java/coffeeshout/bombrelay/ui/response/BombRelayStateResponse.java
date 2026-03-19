package coffeeshout.bombrelay.ui.response;

import coffeeshout.bombrelay.domain.event.BombRelayStateChangedEvent;

public record BombRelayStateResponse(
        String state,
        int currentRound,
        int maxRounds,
        String currentWord,
        String currentTurnPlayerName,
        String eliminatedPlayerName
) {

    public static BombRelayStateResponse from(BombRelayStateChangedEvent event) {
        return new BombRelayStateResponse(
                event.state().name(),
                event.currentRound(),
                event.maxRounds(),
                event.currentWord(),
                event.currentTurnPlayerName(),
                event.eliminatedPlayerName()
        );
    }
}
