package coffeeshout.minigame.event.dto;

import java.util.UUID;

public record MiniGameFinishedEvent(String eventId, String joinCode, String miniGameType) {

    public MiniGameFinishedEvent(String joinCode, String miniGameType) {
        this(UUID.randomUUID().toString(), joinCode, miniGameType);
    }
}
