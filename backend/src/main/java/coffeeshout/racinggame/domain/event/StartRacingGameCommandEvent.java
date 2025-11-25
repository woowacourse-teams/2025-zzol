package coffeeshout.racinggame.domain.event;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import java.time.Instant;
import java.util.UUID;

public record StartRacingGameCommandEvent(
        String eventId,
        RacingGameEventType eventType,
        Instant timestamp,
        TraceInfo traceInfo,
        String joinCode,
        String hostName
) {

    public static StartRacingGameCommandEvent create(String joinCode, String hostName) {
        final String eventId = UUID.randomUUID().toString();
        return new StartRacingGameCommandEvent(
                eventId,
                RacingGameEventType.START_RACING_GAME_COMMAND,
                Instant.now(),
                TraceInfoExtractor.extract(),
                joinCode,
                hostName
        );
    }
}
