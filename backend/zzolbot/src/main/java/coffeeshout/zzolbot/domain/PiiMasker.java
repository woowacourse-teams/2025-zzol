package coffeeshout.zzolbot.domain;

import org.springframework.stereotype.Component;


public class PiiMasker {

    public String mask(String text, PiiMaskingSession session) {
        return session.mask(text);
    }
}
