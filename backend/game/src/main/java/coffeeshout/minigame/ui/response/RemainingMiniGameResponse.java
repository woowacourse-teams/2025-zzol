package coffeeshout.minigame.ui.response;

import java.util.List;

public record RemainingMiniGameResponse(List<String> remaining) {

    public static RemainingMiniGameResponse of(List<String> names) {
        return new RemainingMiniGameResponse(names);
    }
}
