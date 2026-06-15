package coffeeshout.room.domain.event;

import coffeeshout.room.domain.player.Winner;
import java.time.Instant;
import java.util.UUID;

public record RouletteWinnerEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        Winner winner
) {

    public RouletteWinnerEvent(String joinCode, Winner winner) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                winner
        );
    }
}
