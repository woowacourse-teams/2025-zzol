package coffeeshout.room.domain.service;

import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerNameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "용감한", "빠른", "느린", "귀여운", "강한", "작은", "차가운", "따뜻한",
            "활발한", "조용한", "씩씩한", "영리한", "수줍은", "든든한", "명랑한",
            "차분한", "유쾌한", "현명한", "신나는", "대담한", "엉뚱한", "재빠른",
            "날쌘", "슬기로운", "겸손한", "진지한", "솔직한", "다정한", "눈치빠른", "부지런한"
    );

    private static final List<String> NOUNS = List.of(
            "호랑이", "여우", "고양이", "강아지", "토끼", "사자", "펭귄", "독수리",
            "늑대", "판다", "하마", "코끼리", "기린", "치타", "수달", "오리",
            "너구리", "다람쥐", "부엉이", "앵무새", "비버", "악어", "캥거루",
            "코알라", "물개", "하이에나", "고슴도치", "오소리", "미어캣", "라쿤"
    );

    private static final int MAX_RETRY = 50;

    private final WordPicker wordPicker;

    public String generate(Set<String> existingNames) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            final String candidate = wordPicker.pick(ADJECTIVES) + wordPicker.pick(NOUNS);

            if (candidate.length() > PlayerName.MAX_NAME_LENGTH) {
                continue;
            }

            if (!existingNames.contains(candidate)) {
                return candidate;
            }
        }

        throw new InvalidStateException(
                RoomErrorCode.NICKNAME_GENERATION_FAILED,
                "닉네임 생성 실패: 최대 재시도 횟수를 초과했습니다."
        );
    }
}
