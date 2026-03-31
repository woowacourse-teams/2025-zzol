package coffeeshout.numberpoker.infra.messaging.consumer;

import static org.mockito.Mockito.verify;

import coffeeshout.numberpoker.application.NumberPokerService;
import coffeeshout.numberpoker.infra.messaging.event.SettingsCommandEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingsCommandEventConsumerTest {

    @Mock
    NumberPokerService numberPokerService;

    @InjectMocks
    SettingsCommandEventConsumer consumer;

    @Test
    void 이벤트를_수신하면_서비스의_configureRoundCount를_호출한다() {
        SettingsCommandEvent event = new SettingsCommandEvent("ABC123", "꾹이", 5);

        consumer.accept(event);

        verify(numberPokerService).configureRoundCount("ABC123", "꾹이", 5);
    }
}
