package coffeeshout.minigame.ui.command.handler;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.pubsub.PubSubChannelTopic;
import coffeeshout.global.redis.pubsub.PubSubEventPublisher;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import coffeeshout.minigame.ui.request.command.StartMiniGameCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartMiniGameCommandHandler implements MiniGameCommandHandler<StartMiniGameCommand> {

    private final PubSubEventPublisher eventPublisher;

    @Override
    public void handle(String joinCode, StartMiniGameCommand command) {
        final BaseEvent event = new StartMiniGameCommandEvent(joinCode, command.hostName());
        eventPublisher.publishEvent(event, PubSubChannelTopic.MINIGAME.channelTopic());
        log.info("미니게임 시작 이벤트 발행: joinCode={}, hostName={}, eventId={}",
                joinCode, command.hostName(), event.eventId());
    }

    @Override
    public Class<StartMiniGameCommand> getCommandType() {
        return StartMiniGameCommand.class;
    }
}
