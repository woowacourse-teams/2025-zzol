package coffeeshout.websocket;

import coffeeshout.websocket.ui.WebSocketResponse;

public interface RecoveryMessageStore {

    String save(String joinCode, String destination, WebSocketResponse<?> response);
}
