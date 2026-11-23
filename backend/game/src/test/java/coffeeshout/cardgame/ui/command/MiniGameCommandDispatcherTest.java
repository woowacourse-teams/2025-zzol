package coffeeshout.cardgame.ui.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.minigame.ui.command.MiniGameCommand;
import coffeeshout.minigame.ui.command.MiniGameCommandDispatcher;
import coffeeshout.minigame.ui.command.MiniGameCommandHandler;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    void 핸들러가_돌려준_발행_future를_그대로_돌려준다() {
        // given — 핸들러는 Redis Stream 비동기 발행의 future를 돌려준다
        final String joinCode = "TEST123";
        final ValidCommand command = new ValidCommand();
        final CompletableFuture<Void> published = new CompletableFuture<>();

        final MiniGameCommandHandler<ValidCommand> mockHandler = mock(MiniGameCommandHandler.class);
        when(mockHandler.getCommandType()).thenReturn(ValidCommand.class);
        when(mockHandler.handle(joinCode, command)).thenReturn(published);

        final MiniGameCommandDispatcher dispatcher = new MiniGameCommandDispatcher(List.of(mockHandler));

        // when
        final CompletableFuture<Void> dispatched = dispatcher.dispatch(joinCode, command);

        // then — 같은 future여야 발행이 나중에 실패했을 때 Spring이 보낸 사용자에게 에러를 알린다
        assertThat(dispatched).isSameAs(published);
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

    private static class ValidCommand implements MiniGameCommand {}

    private static class InvalidCommand implements MiniGameCommand {}
}
