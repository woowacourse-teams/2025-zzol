package coffeeshout.minigame.event.dto;

import java.util.Map;
import java.util.UUID;

public record MiniGameFinishedEvent(
        String eventId,
        String joinCode,
        String miniGameType,
        Map<String, Integer> ranks,
        int roundCount
) {

    public MiniGameFinishedEvent(String joinCode, String miniGameType, Map<String, Integer> ranks, int roundCount) {
        this(UUID.randomUUID().toString(), joinCode, miniGameType, ranks, roundCount);
    }
}
