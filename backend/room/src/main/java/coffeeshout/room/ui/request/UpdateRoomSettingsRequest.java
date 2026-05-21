package coffeeshout.room.ui.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record UpdateRoomSettingsRequest(
        @DecimalMin("0.1") @DecimalMax("0.9") double adjustmentWeight
) {
}
