package coffeeshout.wormgame.ui.request;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * angle은 라디안 목표각. NaN·무한대는 도메인이 거부한다(INVALID_STEERING). seq는 클라 단조증가 일련번호.
 * 조향 대상(playerName)은 본문에 없다 — STOMP principal 에서 도출한다(WormGameWebSocketController).
 */
public record SteerCommand(double angle, @PositiveOrZero long seq) {}
