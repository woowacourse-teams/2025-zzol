package coffeeshout.cardgame.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record SelectCardCommandEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String playerName,
        Integer cardIndex
) implements BaseEvent {

    public SelectCardCommandEvent(String joinCode, String playerName, Integer cardIndex) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                playerName,
                cardIndex
        );
    }
}
