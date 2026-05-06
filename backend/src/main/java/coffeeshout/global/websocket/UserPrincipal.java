package coffeeshout.global.websocket;

public final class UserPrincipal {

    public static final String PREFIX = "user:";

    private UserPrincipal() {
    }

    public static String of(Long userId) {
        return PREFIX + userId;
    }
}
