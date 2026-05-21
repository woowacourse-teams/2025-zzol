package coffeeshout.room.ui.request;

public record ReadyChangeMessage(
        String joinCode,
        String playerName,
        Boolean isReady
) {
}
