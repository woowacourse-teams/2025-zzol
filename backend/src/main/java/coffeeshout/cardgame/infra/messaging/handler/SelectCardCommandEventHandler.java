package coffeeshout.cardgame.infra.messaging.handler;

import coffeeshout.cardgame.application.CardGameService;
import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import coffeeshout.global.redis.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelectCardCommandEventHandler implements EventHandler<SelectCardCommandEvent> {

    private final CardGameService cardGameService;

    @Override
    public void handle(SelectCardCommandEvent event) {
        cardGameService.selectCard(event.joinCode(), event.playerName(), event.cardIndex());
    }

    @Override
    public Class<SelectCardCommandEvent> eventType() {
        return SelectCardCommandEvent.class;
    }
}
