package coffeeshout.room.application.service.nickname;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.room.domain.RoomErrorCode;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NicknameValidator {

    private final BadWordFiltering badWordFiltering;

    public void validate(String nickname) {
        if (containsProfanity(nickname)) {
            throw new InvalidArgumentException(
                    RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY,
                    "비속어가 포함된 닉네임입니다. 입력값: '" + nickname + "'"
            );
        }
    }

    private boolean containsProfanity(String nickname) {
        return badWordFiltering.check(nickname) || badWordFiltering.blankCheck(nickname);
    }
}
