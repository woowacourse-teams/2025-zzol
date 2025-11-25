package coffeeshout.racinggame.domain.event;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import java.time.Instant;
import java.util.UUID;

public record TapCommandEvent(
        String eventId,
        RacingGameEventType eventType,
        String joinCode,
        String playerName,
        int tapCount,
        Instant timestamp,
        TraceInfo traceInfo
) {

    public static TapCommandEvent create(String joinCode, String playerName, int tapCount) {
        final String eventId = UUID.randomUUID().toString();
        return new TapCommandEvent(
                eventId,
                RacingGameEventType.TAP_COMMAND,
                joinCode,
                playerName,
                tapCount,
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
