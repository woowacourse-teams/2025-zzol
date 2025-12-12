package coffeeshout.racinggame.application;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.pubsub.PubSubChannelTopic;
import coffeeshout.global.redis.pubsub.PubSubEventPublisher;
import coffeeshout.racinggame.domain.event.TapCommandEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RacingGameFacade {

    private final PubSubEventPublisher pubSubEventPublisher;

    public void tap(String joinCode, String hostName, int tapCount) {
        final BaseEvent event = TapCommandEvent.create(joinCode, hostName, tapCount);
        pubSubEventPublisher.publishEvent(event, PubSubChannelTopic.RACING_GAME.channelTopic());
//        log.debug("탭 이벤트 발행: joinCode={}, playerName={}, tapCount={}, eventId={}",
//                joinCode, hostName, tapCount, event.eventId());
    }
}
