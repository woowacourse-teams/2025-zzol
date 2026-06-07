package coffeeshout.blindtimer.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record StopCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        Instant timestamp
) implements BaseEvent {

    public static StopCommandEvent create(String joinCode, String playerName) {
        return new StopCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                Instant.now()
        );
    }
}
