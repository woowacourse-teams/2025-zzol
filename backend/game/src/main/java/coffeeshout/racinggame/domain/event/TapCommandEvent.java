package coffeeshout.racinggame.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record TapCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        int tapCount,
        Instant timestamp
) implements BaseEvent {

    public static TapCommandEvent create(String joinCode, String playerName, int tapCount) {
        final String eventId = UUID.randomUUID().toString();
        return new TapCommandEvent(
                eventId,
                joinCode,
                playerName,
                tapCount,
                Instant.now()
        );
    }
}
