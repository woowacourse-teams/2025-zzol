package coffeeshout.speedtouch.ui.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TouchCommand(
        @NotBlank(message = "플레이어 이름은 필수입니다") String playerName,
        @Min(value = 1, message = "터치 번호는 1 이상이어야 합니다")
        @Max(value = 25, message = "터치 번호는 25 이하여야 합니다")
        int touchedNumber
) {
}
