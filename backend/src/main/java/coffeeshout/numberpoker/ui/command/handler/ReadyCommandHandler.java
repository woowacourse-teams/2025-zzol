package coffeeshout.numberpoker.ui.command.handler;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import coffeeshout.numberpoker.infra.messaging.event.ReadyCommandEvent;
import coffeeshout.numberpoker.ui.command.ReadyCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadyCommandHandler implements MiniGameCommandHandler<ReadyCommand> {

    private final StreamPublisher streamPublisher;

    @Override
    public void handle(String joinCode, ReadyCommand command) {
        final ReadyCommandEvent event = new ReadyCommandEvent(joinCode, command.playerName());
        streamPublisher.publish(StreamKey.NUMBER_POKER_EVENTS, event);
        log.debug("레디 이벤트 발행: joinCode={}, player={}", joinCode, command.playerName());
    }

    @Override
    public Class<ReadyCommand> getCommandType() {
        return ReadyCommand.class;
    }
}
