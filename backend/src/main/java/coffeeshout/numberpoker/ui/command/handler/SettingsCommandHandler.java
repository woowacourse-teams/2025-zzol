package coffeeshout.numberpoker.ui.command.handler;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import coffeeshout.numberpoker.infra.messaging.event.SettingsCommandEvent;
import coffeeshout.numberpoker.ui.command.SettingsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsCommandHandler implements MiniGameCommandHandler<SettingsCommand> {

    private final StreamPublisher streamPublisher;

    @Override
    public void handle(String joinCode, SettingsCommand command) {
        final SettingsCommandEvent event = new SettingsCommandEvent(
                joinCode, command.hostName(), command.roundCount());
        streamPublisher.publish(StreamKey.NUMBER_POKER_EVENTS, event);
        log.debug("설정 이벤트 발행: joinCode={}, host={}, roundCount={}",
                joinCode, command.hostName(), command.roundCount());
    }

    @Override
    public Class<SettingsCommand> getCommandType() {
        return SettingsCommand.class;
    }
}
