package coffeeshout.cardgame.infra.messaging.config;

import coffeeshout.cardgame.application.CardGameService;
import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CardGameStreamEventConsumerConfig {

    private final CardGameService cardGameService;

    @Bean
    public Consumer<SelectCardCommandEvent> selectCardCommandEventConsumer() {
        return event -> cardGameService.selectCard(
                event.joinCode(),
                event.playerName(),
                event.cardIndex()
        );
    }
}
