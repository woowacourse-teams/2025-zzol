package coffeeshout.numberpoker.ui.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.numberpoker.infra.messaging.event.SettingsCommandEvent;
import coffeeshout.numberpoker.ui.command.SettingsCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingsCommandHandlerTest {

    @Mock
    StreamPublisher streamPublisher;

    @InjectMocks
    SettingsCommandHandler handler;

    @Test
    void SettingsCommand_처리_시_SettingsCommandEvent를_스트림에_발행한다() {
        ArgumentCaptor<SettingsCommandEvent> captor = ArgumentCaptor.forClass(SettingsCommandEvent.class);

        handler.handle("ABC123", new SettingsCommand("꾹이", 5));

        verify(streamPublisher).publish(eq(StreamKey.NUMBER_POKER_EVENTS), captor.capture());
        SettingsCommandEvent event = captor.getValue();
        assertThat(event.joinCode()).isEqualTo("ABC123");
        assertThat(event.hostName()).isEqualTo("꾹이");
        assertThat(event.roundCount()).isEqualTo(5);
    }

    @Test
    void 지원하는_커맨드_타입은_SettingsCommand다() {
        assertThat(handler.getCommandType()).isEqualTo(SettingsCommand.class);
    }
}
