package coffeeshout.blindtimer.domain;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.stream.Stream;

public class BlindTimerPlayers {

    private final List<BlindTimerPlayer> players;

    public BlindTimerPlayers(List<Player> roomPlayers) {
        this.players = List.copyOf(
                roomPlayers.stream()
                        .map(BlindTimerPlayer::new)
                        .toList()
        );
    }

    public BlindTimerPlayer findByName(PlayerName name) {
        return players.stream()
                .filter(p -> p.getPlayer().sameName(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 플레이어를 찾을 수 없습니다: " + name.value()));
    }

    public synchronized boolean isAllStopped() {
        return players.stream().allMatch(BlindTimerPlayer::isStopped);
    }

    public Stream<BlindTimerPlayer> stream() {
        return players.stream();
    }

    public List<BlindTimerPlayer> getPlayers() {
        return players;
    }
}
