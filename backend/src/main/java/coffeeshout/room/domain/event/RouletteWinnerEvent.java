package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.room.domain.player.Winner;
import java.time.Instant;

public record RouletteWinnerEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        Winner winner
) implements BaseEvent {

    public RouletteWinnerEvent(String joinCode, Winner winner){
        this(
                java.util.UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                winner
        );
    }
}
