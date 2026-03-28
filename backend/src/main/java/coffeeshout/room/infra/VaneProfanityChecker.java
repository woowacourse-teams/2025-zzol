package coffeeshout.room.infra;

import coffeeshout.room.domain.service.ProfanityChecker;
import com.vane.badwordfiltering.BadWordFiltering;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VaneProfanityChecker implements ProfanityChecker {

    // 특수문자/숫자 우회 제거 패턴
    private static final String BYPASS_CHARS_PATTERN = "[0-9!@#$%^&*()\\-_=+\\[\\]{}|;:',.<>?/`~\\s]";

    private final BadWordFiltering badWordFiltering;

    @Override
    public synchronized boolean contains(String text) {
        return badWordFiltering.check(text)
                || badWordFiltering.blankCheck(text)
                || checkAfterStrippingBypassChars(text);
    }

    @Override
    public synchronized void addAll(List<String> words) {
        badWordFiltering.addAll(words);
    }

    @Override
    public synchronized void add(String word) {
        badWordFiltering.add(word);
    }

    @Override
    public synchronized void remove(String word) {
        badWordFiltering.remove(word);
    }

    private boolean checkAfterStrippingBypassChars(String text) {
        final String stripped = text.replaceAll(BYPASS_CHARS_PATTERN, "");
        if (stripped.equals(text)) {
            return false;
        }
        return badWordFiltering.check(stripped) || badWordFiltering.blankCheck(stripped);
    }
}
