package coffeeshout.speedtouch.ui.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 터치 주인(playerName)은 본문에 없다 — STOMP principal 에서 도출한다(SpeedTouchGameWebSocketController).
 */
public record TouchCommand(
        @Min(value = 1, message = "터치 번호는 1 이상이어야 합니다")
        @Max(value = 25, message = "터치 번호는 25 이하여야 합니다")
        int touchedNumber
) {
}
