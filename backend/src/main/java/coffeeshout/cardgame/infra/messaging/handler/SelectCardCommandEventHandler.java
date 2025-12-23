package coffeeshout.cardgame.infra.messaging.handler;

import coffeeshout.cardgame.application.CardGameService;
import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectCardCommandEventHandler implements Consumer<SelectCardCommandEvent> {

    private final CardGameService cardGameService;

    @Override
    public void accept(SelectCardCommandEvent event) {
        cardGameService.selectCard(
                event.joinCode(),
                event.playerName(),
                event.cardIndex()
        );
    }
}
