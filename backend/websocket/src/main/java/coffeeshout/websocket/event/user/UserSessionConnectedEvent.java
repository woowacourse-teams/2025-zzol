package coffeeshout.websocket.event.user;

public record UserSessionConnectedEvent(Long userId, String sessionId) {
}
