package coffeeshout.blockstacking.domain.event;

import coffeeshout.blockstacking.ui.request.BlockStackingProgressRequest;
import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record BlockStackingCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        int floor,
        double movingBlockX,
        double stackTopX,
        double stackTopWidth,
        Instant timestamp
) implements BaseEvent {

    public static BlockStackingCommandEvent of(
            String joinCode, String authenticatedPlayerName, BlockStackingProgressRequest request
    ) {
        return new BlockStackingCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                authenticatedPlayerName,
                request.floor(),
                request.movingBlockX(),
                request.stackTopX(),
                request.stackTopWidth(),
                Instant.now()
        );
    }
}
