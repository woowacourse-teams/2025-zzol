package coffeeshout.profanity.domain;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TextNormalizer {

    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]");

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        final String nfkc = Normalizer.normalize(text, Form.NFKC);
        final String deLeet = applyLeetSubstitutions(nfkc);
        return SPECIAL_CHARS_PATTERN.matcher(deLeet).replaceAll("").toLowerCase();
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
