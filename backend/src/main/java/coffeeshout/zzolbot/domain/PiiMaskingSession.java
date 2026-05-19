package coffeeshout.zzolbot.domain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PiiMaskingSession {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9+_.%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern IP_PATTERN =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    private final long seed;
    private final ConcurrentHashMap<String, String> emailMap;
    private final ConcurrentHashMap<String, String> ipMap;

    private PiiMaskingSession(long seed) {
        this.seed = seed;
        this.emailMap = new ConcurrentHashMap<>();
        this.ipMap = new ConcurrentHashMap<>();
    }

    public static PiiMaskingSession forSeed(long seed) {
        return new PiiMaskingSession(seed);
    }

    public String mask(String text) {
        if (text == null) {
            return null;
        }
        return maskIp(maskEmail(text));
    }

    private String maskEmail(String text) {
        final Matcher matcher = EMAIL_PATTERN.matcher(text);
        final StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            final String token = emailMap.computeIfAbsent(matcher.group(), e -> "[EMAIL_" + token(seed, e) + "]");
            matcher.appendReplacement(result, Matcher.quoteReplacement(token));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String maskIp(String text) {
        final Matcher matcher = IP_PATTERN.matcher(text);
        final StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            final String token = ipMap.computeIfAbsent(matcher.group(), i -> "[IP_" + token(seed, i) + "]");
            matcher.appendReplacement(result, Matcher.quoteReplacement(token));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String token(long seed, String value) {
        final int hash = (int) (seed ^ value.hashCode());
        return String.format("%04x", hash & 0xFFFF);
    }
}
