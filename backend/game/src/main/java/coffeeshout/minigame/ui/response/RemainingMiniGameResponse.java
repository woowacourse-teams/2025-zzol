package coffeeshout.minigame.ui.response;

import coffeeshout.minigame.domain.MiniGameType;
import java.util.List;

public record RemainingMiniGameResponse(List<String> remaining) {

    public static RemainingMiniGameResponse from(List<MiniGameType> miniGameTypes) {
        return new RemainingMiniGameResponse(miniGameTypes.stream()
                .map(MiniGameType::name)
                .toList());
    }
}
