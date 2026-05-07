package coffeeshout.global.websocket;

import java.util.Objects;

public final class UserPrincipal {

    public static final String PREFIX = "user:";

    private UserPrincipal() {
    }

    public static String of(Long userId) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        return PREFIX + userId;
    }
}
