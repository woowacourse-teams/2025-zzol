package coffeeshout.numberpoker.infra.messaging.consumer;

import static org.mockito.Mockito.verify;

import coffeeshout.numberpoker.application.NumberPokerService;
import coffeeshout.numberpoker.infra.messaging.event.ReadyCommandEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReadyCommandEventConsumerTest {

    @Mock
    NumberPokerService numberPokerService;

    @InjectMocks
    ReadyCommandEventConsumer consumer;

    @Test
    void 이벤트를_수신하면_서비스의_ready를_호출한다() {
        ReadyCommandEvent event = new ReadyCommandEvent("ABC123", "꾹이");

        consumer.accept(event);

        verify(numberPokerService).ready("ABC123", "꾹이");
    }
}
