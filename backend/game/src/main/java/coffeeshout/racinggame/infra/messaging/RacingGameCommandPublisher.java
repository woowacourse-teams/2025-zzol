package coffeeshout.racinggame.infra.messaging;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.racinggame.domain.event.TapCommandEvent;
import coffeeshout.racinggame.infra.RacingGameStreamKey;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RacingGameCommandPublisher {

    private final StreamPublisher streamPublisher;

    public CompletableFuture<Void> tap(String joinCode, String hostName, int tapCount) {
        final BaseEvent event = TapCommandEvent.create(joinCode, hostName, tapCount);
        final CompletableFuture<Void> published = streamPublisher.publishAsync(RacingGameStreamKey.EVENTS, event);
        log.debug(
                "탭 이벤트 발행: joinCode={}, playerName={}, tapCount={}, eventId={}",
                joinCode,
                hostName,
                tapCount,
                event.eventId());
        return published;
    }
}
