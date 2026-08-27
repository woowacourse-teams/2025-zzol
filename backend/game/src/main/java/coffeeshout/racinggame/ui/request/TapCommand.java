package coffeeshout.racinggame.ui.request;

/**
 * 탭 주인(playerName)은 본문에 없다 — STOMP principal 에서 도출한다(RacingGameWebSocketController).
 */
public record TapCommand(int tapCount) {
}
