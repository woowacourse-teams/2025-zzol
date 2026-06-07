package coffeeshout.blockstacking.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record BlockStackingFailEvent(
        String eventId,
        String joinCode,
        String playerName,
        Instant timestamp
) implements BaseEvent {

    public static BlockStackingFailEvent of(String joinCode, String playerName) {
        return new BlockStackingFailEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                Instant.now()
        );
    }
}
