package coffeeshout.blockstacking.domain;

import coffeeshout.gamecommon.Gamer;

public record BlockStackingPlayerProgress(Gamer gamer, int currentFloor, boolean failed) {

    public BlockStackingPlayerProgress {
        if (currentFloor < 0) {
            throw new IllegalArgumentException("currentFloor는 음수일 수 없습니다: " + currentFloor);
        }
    }

    public static BlockStackingPlayerProgress initial(Gamer gamer) {
        return new BlockStackingPlayerProgress(gamer, 0, false);
    }

    public BlockStackingPlayerProgress advanceTo(int floor) {
        if (floor < 0 || floor < currentFloor) {
            throw new IllegalArgumentException(
                    "floor는 currentFloor(" + currentFloor + ") 이상이어야 합니다: " + floor);
        }
        return new BlockStackingPlayerProgress(gamer, floor, this.failed);
    }

    public BlockStackingPlayerProgress fail() {
        return new BlockStackingPlayerProgress(gamer, currentFloor, true);
    }
}
