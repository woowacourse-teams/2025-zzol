package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.room.domain.player.Winner;
import java.time.Instant;
import java.util.UUID;

public record RouletteSpinEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String hostName,
        Winner winner
) implements BaseEvent {

    public RouletteSpinEvent(String joinCode, String hostName, Winner winner) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                hostName,
                winner
        );
    }
}
