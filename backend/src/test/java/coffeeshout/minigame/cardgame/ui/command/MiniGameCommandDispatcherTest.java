package coffeeshout.minigame.cardgame.ui.command;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.minigame.ui.command.MiniGameCommand;
import coffeeshout.minigame.ui.command.MiniGameCommandDispatcher;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import java.util.List;
import org.junit.jupiter.api.Test;

class MiniGameCommandDispatcherTest {

    @Test
    void 해당하는_Command의_handle_메서드가_호출된다() {
        // given
        String joinCode = "TEST123";
        ValidCommand command = new ValidCommand();

        MiniGameCommandHandler<ValidCommand> mockHandler = mock(MiniGameCommandHandler.class);
        when(mockHandler.getCommandType()).thenReturn(ValidCommand.class);

        MiniGameCommandDispatcher dispatcher = new MiniGameCommandDispatcher(List.of(mockHandler));

        // when
        dispatcher.dispatch(joinCode, command);

        // then
        verify(mockHandler).handle(joinCode, command);
    }


    @Test
    void 등록되지_않은_Command에_대해_예외를_발생시킨다() {
        // given
        String joinCode = "TEST123";
        InvalidCommand invalidCommand = new InvalidCommand();

        MiniGameCommandDispatcher dispatcher = new MiniGameCommandDispatcher(List.of());

        // when & then
        assertThatThrownBy(() -> dispatcher.dispatch(joinCode, invalidCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당하는 요청에 대한 게임이 존재하지 않습니다.");
    }

    private static class ValidCommand implements MiniGameCommand {
    }

    private static class InvalidCommand implements MiniGameCommand {
    }
}
