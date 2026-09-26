package coffeeshout.blockstacking.domain;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.SystemException;
import jakarta.annotation.Nullable;

/**
 * @param topX     마지막으로 쌓은 블록의 왼쪽 끝. 아직 한 층도 쌓지 않았으면 null
 * @param topWidth 마지막으로 쌓은 블록의 폭. 아직 한 층도 쌓지 않았으면 null
 */
public record BlockStackingPlayerProgress(
        Gamer gamer,
        int currentFloor,
        boolean failed,
        @Nullable Double topX,
        @Nullable Double topWidth) {

    public BlockStackingPlayerProgress {
        if (currentFloor < 0) {
            throw new SystemException(
                    GlobalErrorCode.INTERNAL_SERVER_ERROR, "currentFloor는 음수일 수 없습니다: " + currentFloor);
        }
    }

    public static BlockStackingPlayerProgress initial(Gamer gamer) {
        return new BlockStackingPlayerProgress(gamer, 0, false, null, null);
    }

    public BlockStackingPlayerProgress advanceTo(int floor, double topX, double topWidth) {
        if (floor < 0 || floor < currentFloor) {
            throw new SystemException(
                    GlobalErrorCode.INTERNAL_SERVER_ERROR,
                    "floor는 currentFloor(" + currentFloor + ") 이상이어야 합니다: " + floor);
        }
        return new BlockStackingPlayerProgress(gamer, floor, this.failed, topX, topWidth);
    }

    public BlockStackingPlayerProgress fail() {
        return new BlockStackingPlayerProgress(gamer, currentFloor, true, topX, topWidth);
    }
}
