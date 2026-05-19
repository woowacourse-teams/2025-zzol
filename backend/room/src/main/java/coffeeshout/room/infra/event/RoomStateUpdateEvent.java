package coffeeshout.room.infra.event;

public record RoomStateUpdateEvent(String joinCode, String reason) {
}
