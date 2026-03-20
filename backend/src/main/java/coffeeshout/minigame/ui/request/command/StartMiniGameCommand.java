package coffeeshout.minigame.ui.request.command;

import coffeeshout.minigame.ui.command.MiniGameCommand;

public record StartMiniGameCommand(String hostName) implements MiniGameCommand {
}
