package coffeeshout.room.ui.request;

public record RoomCreateRequest(
        String hostName,
        Long menuId
) {
}
