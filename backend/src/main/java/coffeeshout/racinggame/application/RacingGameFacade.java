package coffeeshout.racinggame.application;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.racinggame.domain.event.TapCommandEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RacingGameFacade {

    private final StreamPublisher streamPublisher;

    public void tap(String joinCode, String hostName, int tapCount) {
        final BaseEvent event = TapCommandEvent.create(joinCode, hostName, tapCount);
        streamPublisher.publish(StreamKey.RACING_GAME_EVENTS, event);
        log.debug("탭 이벤트 발행: joinCode={}, playerName={}, tapCount={}, eventId={}",
                joinCode, hostName, tapCount, event.eventId());
    }
}
