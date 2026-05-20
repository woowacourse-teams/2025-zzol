package coffeeshout.laddergame.domain;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.room.domain.player.PlayerName;

public record LadderLine(PlayerName playerName, int segmentIndex, int row) {

    public LadderLine {
        if (playerName == null) {
            throw new BusinessException(LadderGameErrorCode.PLAYER_NOT_FOUND,
                    "playerName은 null일 수 없습니다.");
        }
        if (segmentIndex < 0) {
            throw new BusinessException(LadderGameErrorCode.INVALID_SEGMENT_INDEX,
                    "segmentIndex는 0 이상이어야 합니다: " + segmentIndex);
        }
        if (row < 1) {
            throw new BusinessException(LadderGameErrorCode.INVALID_LINE_ROW,
                    "row는 1 이상이어야 합니다: " + row);
        }
    }
}
