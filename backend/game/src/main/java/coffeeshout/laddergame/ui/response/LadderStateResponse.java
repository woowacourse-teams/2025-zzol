package coffeeshout.laddergame.ui.response;

import coffeeshout.laddergame.domain.BottomRanks;
import coffeeshout.laddergame.domain.LadderGameState;
import coffeeshout.laddergame.domain.Poles;
import coffeeshout.minigame.domain.Gamer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public record LadderStateResponse(
        LadderGameState state,
        List<PoleInfo> poles,
        Map<Integer, Integer> bottomRanks,
        Long endTimeEpochMs,
        Map<String, Integer> rankings,
        Long animationDurationMs
) {

    public static LadderStateResponse ofDescription() {
        return new LadderStateResponse(LadderGameState.DESCRIPTION, null, null, null, null, null);
    }

    public static LadderStateResponse ofPrepare(Poles poles, BottomRanks bottomRanks,
                                                Map<Gamer, Integer> colorMap) {
        final List<PoleInfo> poleInfos = poles.getAll().stream()
                .map(p -> {
                    final Gamer gamer = p.gamer();
                    return new PoleInfo(p.index(), gamer.name().value(), colorMap.get(gamer));
                })
                .toList();
        return new LadderStateResponse(LadderGameState.PREPARE, poleInfos, bottomRanks.getAll(), null, null, null);
    }

    public static LadderStateResponse ofDrawing(long endTimeEpochMs) {
        return new LadderStateResponse(LadderGameState.DRAWING, null, null, endTimeEpochMs, null, null);
    }

    public static LadderStateResponse ofResult(Map<String, Integer> rankings, long animationDurationMs) {
        return new LadderStateResponse(LadderGameState.RESULT, null, null, null, rankings, animationDurationMs);
    }

    public static LadderStateResponse ofDone() {
        return new LadderStateResponse(LadderGameState.DONE, null, null, null, null, null);
    }
}
