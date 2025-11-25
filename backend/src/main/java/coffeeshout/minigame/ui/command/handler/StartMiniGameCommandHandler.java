package coffeeshout.minigame.ui.command.handler;

import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.minigame.infra.messaging.MiniGameEventPublisher;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import coffeeshout.minigame.ui.request.command.StartMiniGameCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartMiniGameCommandHandler implements MiniGameCommandHandler<StartMiniGameCommand> {

    private final MiniGameEventPublisher miniGameEventPublisher;

    @Override
    public void handle(String joinCode, StartMiniGameCommand command) {
        final StartMiniGameCommandEvent event = new StartMiniGameCommandEvent(joinCode, command.hostName());
        miniGameEventPublisher.publishEvent(event);
        log.info("미니게임 시작 이벤트 발행: joinCode={}, hostName={}, eventId={}",
                joinCode, command.hostName(), event.eventId());
    }

    @Override
    public Class<StartMiniGameCommand> getCommandType() {
        return StartMiniGameCommand.class;
    }
}
