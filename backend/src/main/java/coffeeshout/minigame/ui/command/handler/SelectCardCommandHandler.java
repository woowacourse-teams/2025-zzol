package coffeeshout.minigame.ui.command.handler;

import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import coffeeshout.minigame.ui.request.command.SelectCardCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelectCardCommandHandler implements MiniGameCommandHandler<SelectCardCommand> {

    private final StreamPublisher streamPublisher;

    @Override
    public void handle(String joinCode, SelectCardCommand command) {
        final BaseEvent event = new SelectCardCommandEvent(joinCode, command.playerName(), command.cardIndex());
        streamPublisher.publish(StreamKey.CARD_GAME_SELECT_BROADCAST, event);
        log.info("카드 선택 이벤트 발행: joinCode={}, playerName={}, cardIndex={}, eventId={}",
                joinCode, command.playerName(), command.cardIndex(), event.eventId());
    }

    @Override
    public Class<SelectCardCommand> getCommandType() {
        return SelectCardCommand.class;
    }
}
