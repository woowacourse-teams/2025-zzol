package coffeeshout.minigame.domain;

import coffeeshout.room.domain.player.PlayerName;

public record Gamer(PlayerName name, Long userId) {

    public boolean isLoggedIn() {
        return userId != null;
    }
}
