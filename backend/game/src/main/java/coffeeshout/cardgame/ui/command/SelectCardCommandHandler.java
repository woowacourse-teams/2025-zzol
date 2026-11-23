package coffeeshout.cardgame.ui.command;

import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import coffeeshout.cardgame.infra.CardGameStreamKey;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import coffeeshout.minigame.ui.request.command.SelectCardCommand;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelectCardCommandHandler implements MiniGameCommandHandler<SelectCardCommand> {

    private final StreamPublisher streamPublisher;

    @Override
    public CompletableFuture<Void> handle(String joinCode, SelectCardCommand command) {
        final BaseEvent event = new SelectCardCommandEvent(joinCode, command.playerName(), command.cardIndex());
        final CompletableFuture<Void> published =
                streamPublisher.publishAsync(CardGameStreamKey.SELECT_BROADCAST, event);
        log.info(
                "카드 선택 이벤트 발행: joinCode={}, playerName={}, cardIndex={}, eventId={}",
                joinCode,
                command.playerName(),
                command.cardIndex(),
                event.eventId());
        return published;
    }

    @Override
    public Class<SelectCardCommand> getCommandType() {
        return SelectCardCommand.class;
    }
}
