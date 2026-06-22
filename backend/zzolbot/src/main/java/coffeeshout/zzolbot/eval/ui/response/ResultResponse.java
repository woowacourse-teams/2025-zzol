package coffeeshout.zzolbot.eval.ui.response;

public record ResultResponse(Long scenarioId, int accuracy, int groundedness, boolean hallucination,
                             String verdict, long latencyMs, int missingToolCalls, String rationale, String answer) {
}
