package coffeeshout.room.domain.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.nickname.NameValidator;
import coffeeshout.profanity.domain.ProfanityChecker;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class PlayerNameValidator implements NameValidator {

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

    @Override
    public void validate(String name) {
        validate(new PlayerName(name));
    }
}
