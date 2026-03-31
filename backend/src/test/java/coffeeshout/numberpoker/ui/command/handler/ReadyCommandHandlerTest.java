package coffeeshout.numberpoker.ui.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.numberpoker.infra.messaging.event.ReadyCommandEvent;
import coffeeshout.numberpoker.ui.command.ReadyCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReadyCommandHandlerTest {

    @Mock
    StreamPublisher streamPublisher;

    @InjectMocks
    ReadyCommandHandler handler;

    @Test
    void ReadyCommand_처리_시_ReadyCommandEvent를_스트림에_발행한다() {
        handler.handle("ABC123", new ReadyCommand("꾹이"));

        verify(streamPublisher).publish(eq(StreamKey.NUMBER_POKER_EVENTS), any(ReadyCommandEvent.class));
    }

    @Test
    void 지원하는_커맨드_타입은_ReadyCommand다() {
        assertThat(handler.getCommandType()).isEqualTo(ReadyCommand.class);
    }
}
