package coffeeshout.profanity.infra.filter;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import org.springframework.stereotype.Component;

@Component
public class TextNormalizer {

    private static final String SPECIAL_CHARS_PATTERN = "[^\\p{L}\\p{N}]";

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        String result = Normalizer.normalize(text, Form.NFC);
        result = applyLeetSubstitutions(result);
        result = result.replaceAll(SPECIAL_CHARS_PATTERN, "");
        return result.toLowerCase();
    }

    private String applyLeetSubstitutions(String text) {
        return text
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('5', 's')
                .replace('7', 't')
                .replace('@', 'a')
                .replace('$', 's');
    }
}
