package coffeeshout.laddergame.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record LadderDrawCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        int segmentIndex,
        Instant timestamp
) implements BaseEvent {

    public static LadderDrawCommandEvent of(String joinCode, String playerName, int segmentIndex) {
        return new LadderDrawCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                segmentIndex,
                Instant.now()
        );
    }
}
