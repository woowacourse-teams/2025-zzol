package coffeeshout.speedtouch.domain;

import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpeedTouchPlayers {

    private final List<SpeedTouchPlayer> players;

    public SpeedTouchPlayers(List<PlayerName> playerNames) {
        this.players = List.copyOf(
                playerNames.stream()
                        .map(SpeedTouchPlayer::new)
                        .toList()
        );
    }

    public SpeedTouchPlayer findByName(PlayerName name) {
        return players.stream()
                .filter(p -> p.getPlayerName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 플레이어를 찾을 수 없습니다: " + name.value()));
    }

    public synchronized boolean isAllFinished() {
        return players.stream().allMatch(SpeedTouchPlayer::isFinished);
    }

    public Map<PlayerName, SpeedTouchPlayer> toPlayerMap() {
        return players.stream()
                .collect(Collectors.toMap(SpeedTouchPlayer::getPlayerName, p -> p));
    }

    public Stream<SpeedTouchPlayer> stream() {
        return players.stream();
    }

    public List<SpeedTouchPlayer> getPlayers() {
        return players;
    }
}
