package coffeeshout.numberpoker.infra.messaging.consumer;

import static org.mockito.Mockito.verify;

import coffeeshout.numberpoker.application.NumberPokerService;
import coffeeshout.numberpoker.infra.messaging.event.FoldCommandEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FoldCommandEventConsumerTest {

    @Mock
    NumberPokerService numberPokerService;

    @InjectMocks
    FoldCommandEventConsumer consumer;

    @Test
    void 이벤트를_수신하면_서비스의_fold를_호출한다() {
        FoldCommandEvent event = new FoldCommandEvent("ABC123", "꾹이");

        consumer.accept(event);

        verify(numberPokerService).fold("ABC123", "꾹이");
    }
}
