package coffeeshout.speedtouch.domain;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpeedTouchPlayers {

    private final List<SpeedTouchPlayer> players;

    public SpeedTouchPlayers(List<Player> roomPlayers) {
        this.players = Collections.synchronizedList(
                roomPlayers.stream()
                        .map(SpeedTouchPlayer::new)
                        .collect(Collectors.toList())
        );
    }

    public SpeedTouchPlayer findByName(PlayerName name) {
        return players.stream()
                .filter(p -> p.getPlayer().sameName(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 플레이어를 찾을 수 없습니다: " + name.value()));
    }

    public boolean isAllFinished() {
        return players.stream().allMatch(SpeedTouchPlayer::isFinished);
    }

    public Map<Player, SpeedTouchPlayer> toPlayerMap() {
        return players.stream()
                .collect(Collectors.toMap(SpeedTouchPlayer::getPlayer, p -> p));
    }

    public Stream<SpeedTouchPlayer> stream() {
        return players.stream();
    }

    public List<SpeedTouchPlayer> getPlayers() {
        return Collections.unmodifiableList(players);
    }
}
