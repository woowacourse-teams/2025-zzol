package coffeeshout.zzolbot.eval.ui.request;

import jakarta.validation.constraints.NotBlank;

public record RecordScenarioRequest(
        @NotBlank String name,
        @NotBlank String question,
        @NotBlank String rubric) {
}
