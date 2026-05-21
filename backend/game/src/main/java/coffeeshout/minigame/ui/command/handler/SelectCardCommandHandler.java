package coffeeshout.minigame.ui.command.handler;

import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import coffeeshout.gamecommon.infra.GameStreamKey;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import coffeeshout.minigame.ui.request.command.SelectCardCommand;
import coffeeshout.redis.BaseEvent;
import coffeeshout.redis.stream.StreamPublisher;
import coffeeshout.websocket.PlayerKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelectCardCommandHandler implements MiniGameCommandHandler<SelectCardCommand> {

    private final StreamPublisher streamPublisher;

    @Override
    public void handle(String joinCode, SelectCardCommand command, PlayerKey playerKey) {
        final BaseEvent event = new SelectCardCommandEvent(
                joinCode, playerKey.playerName(), playerKey.userId(), command.cardIndex()
        );
        streamPublisher.publish(GameStreamKey.CARDGAME_SELECT_BROADCAST, event);
        log.info("카드 선택 이벤트 발행: joinCode={}, playerName={}, cardIndex={}, eventId={}",
                joinCode, playerKey.playerName(), command.cardIndex(), event.eventId());
    }

    @Override
    public Class<SelectCardCommand> getCommandType() {
        return SelectCardCommand.class;
    }
}
