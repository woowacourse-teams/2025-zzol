package coffeeshout.zzolbot.eval.ui.request;

import jakarta.validation.constraints.NotBlank;

public record RunRequest(@NotBlank String label, Integer repeats) {
}
