package coffeeshout.zzolbot.domain;

public class PiiMasker {

    public String mask(String text, PiiMaskingSession session) {
        return session.mask(text);
    }
}
