package coffeeshout.profanity.domain;

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
        final String nfc = Normalizer.normalize(text, Form.NFC);
        final String deLeet = applyLeetSubstitutions(nfc);
        return deLeet.replaceAll(SPECIAL_CHARS_PATTERN, "").toLowerCase();
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
