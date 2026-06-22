package coffeeshout.zzolbot.eval.ui.response;

public record ScenarioResponse(Long id, String name, String question, String rubric, String sourceType,
                               String createdAt) {
}
