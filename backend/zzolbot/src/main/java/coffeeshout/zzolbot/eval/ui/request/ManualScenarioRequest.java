package coffeeshout.zzolbot.eval.ui.request;

import jakarta.validation.constraints.NotBlank;

public record ManualScenarioRequest(
        @NotBlank String name,
        @NotBlank String question,
        @NotBlank String snapshotJson,
        @NotBlank String rubric) {
}
