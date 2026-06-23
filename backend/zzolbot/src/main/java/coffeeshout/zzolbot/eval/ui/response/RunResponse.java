package coffeeshout.zzolbot.eval.ui.response;

public record RunResponse(Long id, String label, String model, String status,
                          int scenarioCount, int passCount, String startedAt, String finishedAt) {
}
