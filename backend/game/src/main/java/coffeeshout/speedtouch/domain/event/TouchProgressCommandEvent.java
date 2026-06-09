package coffeeshout.speedtouch.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record TouchProgressCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        int touchedNumber,
        Instant timestamp
) implements BaseEvent {

    public static TouchProgressCommandEvent create(String joinCode, String playerName, int touchedNumber) {
        return new TouchProgressCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                touchedNumber,
                Instant.now()
        );
    }
}
