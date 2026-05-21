package coffeeshout.minigame.domain;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Gamer(PlayerName name, Long userId) {

    public Gamer {
        Objects.requireNonNull(name, "name must not be null");
    }

    public static Gamer guest(PlayerName name) {
        return new Gamer(name, null);
    }

    public static Gamer loggedIn(PlayerName name, long userId) {
        return new Gamer(name, userId);
    }

    public boolean isLoggedIn() {
        return userId != null;
    }

    public Optional<Long> userIdOptional() {
        return Optional.ofNullable(userId);
    }

    public void validateAgainst(List<Gamer> registeredGamers) {
        boolean valid = registeredGamers.stream()
                .anyMatch(g -> g.name().equals(this.name)
                        && (!g.isLoggedIn() || Objects.equals(g.userId(), this.userId)));
        if (!valid) {
            throw new BusinessException(GamerErrorCode.UNAUTHORIZED_GAMER,
                    "등록되지 않은 게이머이거나 권한이 없습니다: " + this.name.value());
        }
    }
}
