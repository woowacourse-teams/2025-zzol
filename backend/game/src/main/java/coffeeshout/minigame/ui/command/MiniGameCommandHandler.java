package coffeeshout.minigame.ui.command;

import coffeeshout.websocket.PlayerKey;

public interface MiniGameCommandHandler<T extends MiniGameCommand> {

    void handle(String joinCode, T command, PlayerKey playerKey);

    Class<T> getCommandType();
}
