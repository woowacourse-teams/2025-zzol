package coffeeshout.blindtimer.domain;

import coffeeshout.gamecommon.Gamer;
import java.util.List;
import java.util.stream.Stream;

public class BlindTimerPlayers {

    private final List<BlindTimerPlayer> players;

    public BlindTimerPlayers(List<Gamer> gamers) {
        this.players = List.copyOf(
                gamers.stream()
                        .map(BlindTimerPlayer::new)
                        .toList()
        );
    }

    public BlindTimerPlayer findByName(String name) {
        return players.stream()
                .filter(p -> p.getGamer().getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 플레이어를 찾을 수 없습니다: " + name));
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
