package coffeeshout.blockstacking.domain;

import coffeeshout.room.domain.player.PlayerName;

public record BlockStackingPlayerProgress(PlayerName playerName, int currentFloor, boolean failed) {

    public BlockStackingPlayerProgress {
        if (currentFloor < 0) {
            throw new IllegalArgumentException("currentFloor는 음수일 수 없습니다: " + currentFloor);
        }
    }

    public static BlockStackingPlayerProgress initial(PlayerName playerName) {
        return new BlockStackingPlayerProgress(playerName, 0, false);
    }

    public BlockStackingPlayerProgress advanceTo(int floor) {
        if (floor < 0 || floor < currentFloor) {
            throw new IllegalArgumentException(
                    "floor는 currentFloor(" + currentFloor + ") 이상이어야 합니다: " + floor);
        }
        return new BlockStackingPlayerProgress(playerName, floor, false);
    }

    public BlockStackingPlayerProgress fail() {
        return new BlockStackingPlayerProgress(playerName, currentFloor, true);
    }
}
