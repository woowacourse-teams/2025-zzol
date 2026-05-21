package coffeeshout.room.domain.player;

import coffeeshout.room.domain.roulette.Probability;
import java.util.Objects;
import lombok.Getter;

@Getter
public class Player {

    private final PlayerName name;
    private final Long userId;
    private final String userCode;
    private PlayerType playerType;
    private Boolean isReady;
    private Integer colorIndex;
    private Probability probability;

    private Player(PlayerName name, Long userId, String userCode, Boolean isReady, PlayerType playerType) {
        this.name = name;
        this.userId = userId;
        this.userCode = userCode;
        this.playerType = playerType;
        this.isReady = isReady;
    }

    public static Player createHost(PlayerName name) {
        return new Player(name, null, null, true, PlayerType.HOST);
    }

    public static Player createHost(PlayerName name, Long userId) {
        return new Player(name, userId, null, true, PlayerType.HOST);
    }

    public static Player createHost(PlayerName name, Long userId, String userCode) {
        return new Player(name, userId, userCode, true, PlayerType.HOST);
    }

    public static Player createGuest(PlayerName name) {
        return new Player(name, null, null, false, PlayerType.GUEST);
    }

    public static Player createGuest(PlayerName name, Long userId) {
        return new Player(name, userId, null, false, PlayerType.GUEST);
    }

    public static Player createGuest(PlayerName name, Long userId, String userCode) {
        return new Player(name, userId, userCode, false, PlayerType.GUEST);
    }

    public boolean sameName(PlayerName playerName) {
        return Objects.equals(name, playerName);
    }

    public void updateReadyState(Boolean isReady) {
        this.isReady = isReady;
    }

    public void updateProbability(Probability probability) {
        this.probability = probability;
    }

    public void promote() {
        this.playerType = PlayerType.HOST;
        this.isReady = true;
    }

    public void assignColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Player player)) {
            return false;
        }
        return Objects.equals(name, player.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
