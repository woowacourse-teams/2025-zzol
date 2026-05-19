package coffeeshout.user.ui.request;

import jakarta.validation.constraints.NotBlank;

public record ExchangeCodeRequest(
        @NotBlank String code
) {
}
