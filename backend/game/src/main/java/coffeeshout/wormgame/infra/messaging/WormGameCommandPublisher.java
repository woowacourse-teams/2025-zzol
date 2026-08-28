package coffeeshout.wormgame.infra.messaging;

import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.wormgame.domain.event.SteerCommandEvent;
import coffeeshout.wormgame.infra.WormGameStreamKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WormGameCommandPublisher {

    private final StreamPublisher streamPublisher;

    public void steer(String joinCode, String playerName, double angle, long seq) {
        final SteerCommandEvent event = SteerCommandEvent.create(joinCode, playerName, angle, seq);
        streamPublisher.publish(WormGameStreamKey.EVENTS, event);
        log.debug("조향 이벤트 발행: joinCode={}, playerName={}, seq={}", joinCode, playerName, seq);
    }
}
