package coffeeshout.gamecommon;

import java.util.Objects;

public record Gamer(String name, Long userId) {

    public Gamer {
        Objects.requireNonNull(name, "name must not be null");
    }

    public static Gamer guest(String name) {
        return new Gamer(name, null);
    }

    public static Gamer loggedIn(String name, Long userId) {
        return new Gamer(name, userId);
    }

    public static Gamer of(String name, Long userId) {
        return userId != null ? loggedIn(name, userId) : guest(name);
    }

    public boolean isLoggedIn() {
        return userId != null;
    }
}
