package coffeeshout.room.infra.websocket.event;

/**
 * @param reason "PLAYER_REMOVED", "PLAYER_RECONNECTED" 등
 */
public record RoomStateUpdateEvent(String joinCode, String reason) {
}
