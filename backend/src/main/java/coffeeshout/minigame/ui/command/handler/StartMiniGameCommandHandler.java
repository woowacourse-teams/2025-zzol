package coffeeshout.minigame.ui.command.handler;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
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

    private final StreamPublisher streamPublisher;

    @Override
    public void handle(String joinCode, StartMiniGameCommand command) {
        final BaseEvent event = new StartMiniGameCommandEvent(joinCode, command.hostName());
        streamPublisher.publish(StreamKey.MINIGAME_EVENTS, event);
    }

    @Override
    public Class<StartMiniGameCommand> getCommandType() {
        return StartMiniGameCommand.class;
    }
}
