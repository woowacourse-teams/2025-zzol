package coffeeshout.minigame.ui.request.command;

import coffeeshout.minigame.ui.command.MiniGameCommand;

public record SelectCardCommand(String playerName, Integer cardIndex) implements MiniGameCommand {
}
