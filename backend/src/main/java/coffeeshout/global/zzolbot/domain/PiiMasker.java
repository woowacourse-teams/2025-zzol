package coffeeshout.global.zzolbot.domain;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PiiMasker {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9+_.%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    private static final Pattern IP_PATTERN =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    public String mask(String text) {
        if (text == null) {
            return null;
        }
        final String emailMasked = EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL]");
        return IP_PATTERN.matcher(emailMasked).replaceAll("[IP]");
    }
}
