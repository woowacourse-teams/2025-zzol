package coffeeshout.room.application.service.nickname;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.room.domain.RoomErrorCode;
import com.vane.badwordfiltering.BadWordFiltering;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NicknameValidator {

    // 한글 자모·음절, 영숫자만 허용 — 나머지는 특수문자 삽입 우회로 간주
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9]");

    // 영어 리트스피크: 숫자·기호 → 알파벳 치환 (sh1t → shit, @ss → ass)
    private static final Map<Character, Character> LEET_MAP = Map.of(
            '0', 'o', '1', 'i', '3', 'e', '4', 'a',
            '5', 's', '7', 't', '@', 'a', '$', 's', '!', 'i'
    );

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
        // 1. 원본 검사
        if (checkBadWord(nickname)) return true;

        // 2. 특수문자 제거 후 재검사 — 한국어 삽입 우회 대응 (씨@@@발 → 씨발)
        String stripped = SPECIAL_CHARS.matcher(nickname).replaceAll("");
        if (!stripped.equals(nickname) && checkBadWord(stripped)) return true;

        // 3. 리트스피크 정규화 후 재검사 — 영어 우회 대응 (sh1t → shit, @ss → ass)
        String leetNormalized = normalizeLeet(nickname);
        return !leetNormalized.equals(nickname) && checkBadWord(leetNormalized);
    }

    private boolean checkBadWord(String text) {
        return !text.isEmpty() && (badWordFiltering.check(text) || badWordFiltering.blankCheck(text));
    }

    private String normalizeLeet(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            sb.append(LEET_MAP.getOrDefault(c, c));
        }
        return sb.toString();
    }
}
