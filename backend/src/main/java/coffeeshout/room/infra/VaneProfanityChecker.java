package coffeeshout.room.infra;

import coffeeshout.room.domain.service.ProfanityChecker;
import com.vane.badwordfiltering.BadWordFiltering;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VaneProfanityChecker implements ProfanityChecker {

    // 자음 축약 패턴 직접 차단
    private static final Map<String, String> CONSONANT_ABBREVIATIONS = Map.of(
            "ㅅㅂ", "씨발",
            "ㅂㅅ", "병신",
            "ㅈㄹ", "지랄",
            "ㅄ", "병신",
            "ㅁㅊ", "미친"
    );

    // 특수문자/숫자 우회 제거 패턴
    private static final String BYPASS_CHARS_PATTERN = "[0-9!@#$%^&*()\\-_=+\\[\\]{}|;:',.<>?/`~\\.\\s]";

    private final BadWordFiltering badWordFiltering;

    @Override
    public boolean contains(String text) {
        return badWordFiltering.check(text)
                || badWordFiltering.blankCheck(text)
                || checkConsonantAbbreviations(text)
                || checkAfterStrippingBypassChars(text);
    }

    private boolean checkConsonantAbbreviations(String text) {
        return CONSONANT_ABBREVIATIONS.keySet().stream()
                .anyMatch(text::contains);
    }

    private boolean checkAfterStrippingBypassChars(String text) {
        String stripped = text.replaceAll(BYPASS_CHARS_PATTERN, "");
        if (stripped.equals(text)) {
            return false;
        }
        return badWordFiltering.check(stripped) || badWordFiltering.blankCheck(stripped);
    }
}
