package coffeeshout.profanity.domain;

public enum Language {
    KOREAN,
    ENGLISH;

    public static Language detect(String text) {
        if (text == null || text.isBlank()) {
            return ENGLISH;
        }
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if ((c >= '가' && c <= '힣')
                    || (c >= 'ᄀ' && c <= 'ᇿ')
                    || (c >= '㄰' && c <= '㆏')
                    || (c >= 'ꥠ' && c <= '꥿')
                    || (c >= 'ힰ' && c <= '퟿')) {
                return KOREAN;
            }
        }
        return ENGLISH;
    }
}
