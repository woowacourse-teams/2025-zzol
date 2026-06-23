package coffeeshout.zzolbot.eval.ui.response;

import java.util.List;

public record RunDetailResponse(RunResponse run, List<ResultResponse> results) {
}
