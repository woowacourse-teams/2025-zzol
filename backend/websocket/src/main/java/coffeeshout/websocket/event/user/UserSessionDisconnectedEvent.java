package coffeeshout.websocket.event.user;

public record UserSessionDisconnectedEvent(Long userId, String sessionId) {
}
