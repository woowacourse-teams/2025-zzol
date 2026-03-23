package coffeeshout.room.domain.service;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.player.PlayerName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerNameValidator {

    private final ProfanityChecker profanityChecker;

    public void validate(PlayerName playerName) {
        if (profanityChecker.contains(playerName.value())) {
            throw new InvalidArgumentException(
                    RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY,
                    "비속어가 포함된 닉네임입니다. 입력값: '" + playerName + "'"
            );
        }
    }
}
