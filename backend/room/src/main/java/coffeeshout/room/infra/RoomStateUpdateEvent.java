package coffeeshout.room.infra;

public record RoomStateUpdateEvent(String joinCode, String reason) {
}
