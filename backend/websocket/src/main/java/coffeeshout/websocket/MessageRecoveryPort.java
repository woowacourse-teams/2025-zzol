package coffeeshout.websocket;

import coffeeshout.websocket.ui.WebSocketResponse;

public interface MessageRecoveryPort {

    String save(String joinCode, String destination, WebSocketResponse<?> response);
}
