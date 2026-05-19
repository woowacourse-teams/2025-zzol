package coffeeshout.websocket;

import java.util.Objects;

public final class UserPrincipal {

    public static final String PREFIX = "user:";

    private UserPrincipal() {
    }

    public static String of(Long userId) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        return PREFIX + userId;
    }

    public static Long extractUserId(java.security.Principal principal) {
        if (principal == null || !principal.getName().startsWith(PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(principal.getName().substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
