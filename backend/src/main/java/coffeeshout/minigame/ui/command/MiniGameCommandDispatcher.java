package coffeeshout.minigame.ui.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MiniGameCommandDispatcher {

    private final Map<Class<? extends MiniGameCommand>, MiniGameCommandHandler<?>> handlers;

    public MiniGameCommandDispatcher(List<MiniGameCommandHandler<?>> handlerList) {
        handlers = new HashMap<>();
        for (MiniGameCommandHandler<?> handler : handlerList) {
            handlers.put(handler.getCommandType(), handler);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends MiniGameCommand> void dispatch(String joinCode, T command) {
        final MiniGameCommandHandler<T> handler = (MiniGameCommandHandler<T>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("해당하는 요청에 대한 게임이 존재하지 않습니다.");
        }
        handler.handle(joinCode, command);
    }
}
