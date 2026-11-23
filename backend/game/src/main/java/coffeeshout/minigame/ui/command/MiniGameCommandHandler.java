package coffeeshout.minigame.ui.command;

import java.util.concurrent.CompletableFuture;

public interface MiniGameCommandHandler<T extends MiniGameCommand> {

    CompletableFuture<Void> handle(String joinCode, T command);

    Class<T> getCommandType();
}
