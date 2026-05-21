package coffeeshout.laddergame.domain;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;

public record Pole(int index, Gamer gamer) {

    public Pole {
        if (gamer == null) {
            throw new BusinessException(LadderGameErrorCode.PLAYER_NOT_FOUND, "플레이어는 null일 수 없습니다");
        }
        if (index < 0) {
            throw new BusinessException(LadderGameErrorCode.INVALID_POLE_INDEX,
                    "기둥 인덱스는 0 이상이어야 합니다: " + index);
        }
    }
}
