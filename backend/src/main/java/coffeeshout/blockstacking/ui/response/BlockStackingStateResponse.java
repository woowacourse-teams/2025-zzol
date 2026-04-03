package coffeeshout.blockstacking.ui.response;

import coffeeshout.blockstacking.domain.BlockStackingGameState;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public record BlockStackingStateResponse(
        BlockStackingGameState state,
        @JsonInclude(Include.NON_NULL) Long endTimeEpochMs
) {

    public static BlockStackingStateResponse of(BlockStackingGameState state) {
        return new BlockStackingStateResponse(state, null);
    }

    public static BlockStackingStateResponse ofPlaying(long endTimeEpochMs) {
        return new BlockStackingStateResponse(BlockStackingGameState.PLAYING, endTimeEpochMs);
    }
}
