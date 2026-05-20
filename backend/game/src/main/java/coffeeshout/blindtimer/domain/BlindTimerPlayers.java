package coffeeshout.blindtimer.domain;

import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.stream.Stream;

public class BlindTimerPlayers {

    private final List<BlindTimerPlayer> players;

    public BlindTimerPlayers(List<PlayerName> playerNames) {
        this.players = List.copyOf(
                playerNames.stream()
                        .map(BlindTimerPlayer::new)
                        .toList()
        );
    }

    public BlindTimerPlayer findByName(PlayerName name) {
        return players.stream()
                .filter(p -> p.getPlayerName().equals(name))
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
