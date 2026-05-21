package coffeeshout.room.ui.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record UpdateRoomSettingsRequest(
        @NotBlank String hostName,
        @DecimalMin("0.1") @DecimalMax("0.9") double adjustmentWeight
) {
}
