package coffeeshout.room.ui.response;

import coffeeshout.room.domain.Playable;
import java.util.List;

public record RemainingMiniGameResponse(List<String> remaining) {

    public static RemainingMiniGameResponse from(List<Playable> miniGames) {
        return new RemainingMiniGameResponse(miniGames.stream()
                .map(miniGame -> miniGame.getMiniGameType().name())
                .toList());
    }
}
