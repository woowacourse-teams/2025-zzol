package coffeeshout.speedtouch.domain;

import coffeeshout.gamecommon.Gamer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpeedTouchPlayers {

    private final List<SpeedTouchPlayer> players;

    public SpeedTouchPlayers(List<Gamer> gamers) {
        this.players = List.copyOf(
                gamers.stream()
                        .map(SpeedTouchPlayer::new)
                        .toList()
        );
    }

    public SpeedTouchPlayer findByName(String name) {
        return players.stream()
                .filter(p -> p.getGamer().getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 플레이어를 찾을 수 없습니다: " + name));
    }

    public synchronized boolean isAllFinished() {
        return players.stream().allMatch(SpeedTouchPlayer::isFinished);
    }

    public Map<Gamer, SpeedTouchPlayer> toGamerMap() {
        return players.stream()
                .collect(Collectors.toMap(SpeedTouchPlayer::getGamer, p -> p));
    }

    public Stream<SpeedTouchPlayer> stream() {
        return players.stream();
    }

    public List<SpeedTouchPlayer> getPlayers() {
        return players;
    }
}
