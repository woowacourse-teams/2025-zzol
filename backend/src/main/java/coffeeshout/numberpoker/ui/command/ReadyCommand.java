package coffeeshout.numberpoker.ui.command;

import coffeeshout.minigame.ui.command.MiniGameCommand;

public record ReadyCommand(String playerName) implements MiniGameCommand {
}
