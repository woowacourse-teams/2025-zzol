package coffeeshout.numberpoker.ui;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.numberpoker.infra.messaging.event.FoldCommandEvent;
import coffeeshout.numberpoker.infra.messaging.event.ReadyCommandEvent;
import coffeeshout.numberpoker.infra.messaging.event.SettingsCommandEvent;
import coffeeshout.numberpoker.ui.command.FoldCommand;
import coffeeshout.numberpoker.ui.command.ReadyCommand;
import coffeeshout.numberpoker.ui.command.SettingsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NumberPokerWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/poker/fold")
    public void fold(@DestinationVariable String joinCode, @Payload FoldCommand command) {
        streamPublisher.publish(StreamKey.NUMBER_POKER_EVENTS, new FoldCommandEvent(joinCode, command.playerName()));
        log.debug("폴드 요청: joinCode={}, player={}", joinCode, command.playerName());
    }

    @MessageMapping("/room/{joinCode}/poker/ready")
    public void ready(@DestinationVariable String joinCode, @Payload ReadyCommand command) {
        streamPublisher.publish(StreamKey.NUMBER_POKER_EVENTS, new ReadyCommandEvent(joinCode, command.playerName()));
        log.debug("레디 요청: joinCode={}, player={}", joinCode, command.playerName());
    }

    @MessageMapping("/room/{joinCode}/poker/settings")
    public void settings(@DestinationVariable String joinCode, @Payload SettingsCommand command) {
        streamPublisher.publish(StreamKey.NUMBER_POKER_EVENTS,
                new SettingsCommandEvent(joinCode, command.hostName(), command.roundCount()));
        log.debug("설정 요청: joinCode={}, host={}, roundCount={}",
                joinCode, command.hostName(), command.roundCount());
    }
}
