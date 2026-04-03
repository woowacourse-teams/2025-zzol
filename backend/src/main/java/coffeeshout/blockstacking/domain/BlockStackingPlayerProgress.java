package coffeeshout.blockstacking.domain;

import coffeeshout.room.domain.player.PlayerName;

public record BlockStackingPlayerProgress(PlayerName playerName, int currentFloor) {

    public static BlockStackingPlayerProgress initial(PlayerName playerName) {
        return new BlockStackingPlayerProgress(playerName, 0);
    }

    public BlockStackingPlayerProgress advanceTo(int floor) {
        return new BlockStackingPlayerProgress(playerName, floor);
    }
}
