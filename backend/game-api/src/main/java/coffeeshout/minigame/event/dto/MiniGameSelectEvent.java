package coffeeshout.minigame.event.dto;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.minigame.domain.MiniGameType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MiniGameSelectEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String hostName,
        List<MiniGameType> miniGameTypes
) implements BaseEvent {

    public MiniGameSelectEvent(String joinCode, String hostName, List<MiniGameType> miniGameTypes) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                hostName,
                miniGameTypes
        );
    }
}
