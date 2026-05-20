package coffeeshout.laddergame.domain;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.room.domain.player.PlayerName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Poles {

    private final List<Pole> poles;

    private Poles(List<Pole> poles) {
        this.poles = List.copyOf(poles);
    }

    public static Poles assign(List<PlayerName> playerNames) {
        final List<PlayerName> shuffled = new ArrayList<>(playerNames);
        Collections.shuffle(shuffled);
        final List<Pole> assigned = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i++) {
            assigned.add(new Pole(i, shuffled.get(i)));
        }
        return new Poles(assigned);
    }

    public int getPoleIndex(PlayerName playerName) {
        return poles.stream()
                .filter(p -> p.playerName().equals(playerName))
                .mapToInt(Pole::index)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        LadderGameErrorCode.PLAYER_NOT_FOUND,
                        "플레이어를 찾을 수 없습니다: " + playerName.value()
                ));
    }

    public PlayerName getPlayerName(int poleIndex) {
        return poles.stream()
                .filter(p -> p.index() == poleIndex)
                .map(Pole::playerName)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        LadderGameErrorCode.INVALID_POLE_INDEX,
                        "기둥 인덱스가 유효하지 않습니다: " + poleIndex
                ));
    }

    public int size() {
        return poles.size();
    }

    public boolean isValidSegment(int segmentIndex) {
        return segmentIndex >= 0 && segmentIndex <= size() - 2;
    }

    public boolean contains(PlayerName playerName) {
        return poles.stream().anyMatch(p -> p.playerName().equals(playerName));
    }

    public List<Pole> getAll() {
        return poles;
    }
}
