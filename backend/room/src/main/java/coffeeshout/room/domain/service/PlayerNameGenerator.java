package coffeeshout.room.domain.service;

import coffeeshout.global.exception.custom.SystemException;
import coffeeshout.global.nickname.RandomNameWordPool;
import coffeeshout.global.nickname.WordPicker;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Set;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Service
public class PlayerNameGenerator {

    private static final int MAX_RETRY = 50;

    private final WordPicker wordPicker;

    public PlayerName generate(Set<String> existingNames) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            final String candidate = wordPicker.pick(RandomNameWordPool.ADJECTIVES)
                    + wordPicker.pick(RandomNameWordPool.NOUNS);

            if (candidate.length() > PlayerName.MAX_NAME_LENGTH) {
                continue;
            }

            if (!existingNames.contains(candidate)) {
                return new PlayerName(candidate);
            }
        }

        throw new SystemException(
                RoomErrorCode.PLAYER_NAME_GENERATION_FAILED,
                "닉네임 생성 실패: 최대 재시도 횟수를 초과했습니다."
        );
    }
}
