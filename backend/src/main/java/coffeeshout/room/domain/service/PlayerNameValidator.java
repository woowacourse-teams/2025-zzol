package coffeeshout.room.domain.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerNameValidator {

    private final ProfanityChecker profanityChecker;

    public void validate(PlayerName playerName) {
        Objects.requireNonNull(playerName, "playerName은 null일 수 없습니다.");

        if (profanityChecker.contains(playerName.value())) {
            throw new BusinessException(
                    RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY,
                    "비속어가 포함된 닉네임입니다. 입력값: '" + playerName + "'"
            );
        }
    }
}
