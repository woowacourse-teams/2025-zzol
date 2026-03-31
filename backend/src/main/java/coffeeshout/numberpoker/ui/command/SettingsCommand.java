package coffeeshout.numberpoker.ui.command;

import coffeeshout.minigame.ui.command.MiniGameCommand;

public record SettingsCommand(String hostName, int roundCount) implements MiniGameCommand {
}
