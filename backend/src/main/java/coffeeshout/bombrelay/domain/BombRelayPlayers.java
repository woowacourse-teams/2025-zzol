package coffeeshout.bombrelay.domain;

import coffeeshout.room.domain.player.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class BombRelayPlayers {

    private final List<BombRelayPlayer> players;

    public BombRelayPlayers(List<Player> roomPlayers) {
        final List<BombRelayPlayer> mutable = new ArrayList<>(
                roomPlayers.stream().map(BombRelayPlayer::new).toList()
        );
        Collections.shuffle(mutable);
        this.players = mutable;
    }

    public List<BombRelayPlayer> getSurvivors() {
        return players.stream()
                .filter(p -> !p.isEliminated())
                .toList();
    }

    public BombRelayPlayer getByTurnIndex(int turnIndex) {
        final List<BombRelayPlayer> survivors = getSurvivors();
        return survivors.get(turnIndex % survivors.size());
    }

    public int survivorCount() {
        return (int) players.stream().filter(p -> !p.isEliminated()).count();
    }

    public Stream<BombRelayPlayer> stream() {
        return players.stream();
    }

    public List<BombRelayPlayer> getAll() {
        return List.copyOf(players);
    }

    public int calculateMaxRounds() {
        final int size = players.size();
        if (size <= 2) return 1;
        if (size == 3) return 2;
        return 3;
    }
}
