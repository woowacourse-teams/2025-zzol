package coffeeshout.bombrelay.domain;

import coffeeshout.room.domain.player.Player;
import lombok.Getter;

@Getter
public class BombRelayPlayer {

    private final Player player;
    private boolean eliminated;
    private int eliminatedRound;

    public BombRelayPlayer(Player player) {
        this.player = player;
    }

    public void eliminate(int round) {
        this.eliminated = true;
        this.eliminatedRound = round;
    }

    public String getName() {
        return player.getName().value();
    }
}
