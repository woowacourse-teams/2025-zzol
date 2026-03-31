package coffeeshout.numberpoker.ui.command;

import coffeeshout.minigame.ui.command.MiniGameCommand;

public record FoldCommand(String playerName) implements MiniGameCommand {
}
