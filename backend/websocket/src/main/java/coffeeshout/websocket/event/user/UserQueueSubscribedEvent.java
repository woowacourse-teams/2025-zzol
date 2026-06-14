package coffeeshout.websocket.event.user;

public record UserQueueSubscribedEvent(Long userId, String destination) {
}
