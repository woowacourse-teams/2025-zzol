package coffeeshout.numberpoker.ui.command.handler;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import coffeeshout.numberpoker.infra.messaging.event.FoldCommandEvent;
import coffeeshout.numberpoker.ui.command.FoldCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FoldCommandHandler implements MiniGameCommandHandler<FoldCommand> {

    private final StreamPublisher streamPublisher;

    @Override
    public void handle(String joinCode, FoldCommand command) {
        final FoldCommandEvent event = new FoldCommandEvent(joinCode, command.playerName());
        streamPublisher.publish(StreamKey.NUMBER_POKER_EVENTS, event);
        log.debug("폴드 이벤트 발행: joinCode={}, player={}", joinCode, command.playerName());
    }

    @Override
    public Class<FoldCommand> getCommandType() {
        return FoldCommand.class;
    }
}
