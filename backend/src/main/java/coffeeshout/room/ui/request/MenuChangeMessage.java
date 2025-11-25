package coffeeshout.room.ui.request;

public record MenuChangeMessage(
        String playerName,
        Long menuId
) {
}
