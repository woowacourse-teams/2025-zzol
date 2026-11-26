package coffeeshout.wormgame.infra.messaging;

import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.wormgame.domain.event.SteerCommandEvent;
import coffeeshout.wormgame.infra.WormGameStreamKey;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WormGameCommandPublisher {

    private final StreamPublisher streamPublisher;

    public CompletableFuture<Void> steer(String joinCode, String playerName, double angle, long seq) {
        final SteerCommandEvent event = SteerCommandEvent.create(joinCode, playerName, angle, seq);
        final CompletableFuture<Void> published = streamPublisher.publishAsync(WormGameStreamKey.EVENTS, event);
        log.debug("조향 이벤트 발행: joinCode={}, playerName={}, seq={}", joinCode, playerName, seq);
        return published;
    }
}
