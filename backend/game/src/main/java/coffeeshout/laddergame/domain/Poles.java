package coffeeshout.laddergame.domain;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Poles {

    private final List<Pole> poles;

    private Poles(List<Pole> poles) {
        this.poles = List.copyOf(poles);
    }

    public static Poles assign(List<Gamer> gamers) {
        final List<Gamer> shuffled = new ArrayList<>(gamers);
        Collections.shuffle(shuffled);
        final List<Pole> assigned = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i++) {
            assigned.add(new Pole(i, shuffled.get(i)));
        }
        return new Poles(assigned);
    }

    public int getPoleIndex(Gamer gamer) {
        return poles.stream()
                .filter(p -> p.gamer().equals(gamer))
                .mapToInt(Pole::index)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        LadderGameErrorCode.PLAYER_NOT_FOUND,
                        "플레이어를 찾을 수 없습니다: " + gamer.name().value()
                ));
    }

    public Gamer getGamer(int poleIndex) {
        return poles.stream()
                .filter(p -> p.index() == poleIndex)
                .map(Pole::gamer)
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

    public boolean contains(Gamer gamer) {
        return poles.stream().anyMatch(p -> p.gamer().equals(gamer));
    }

    public List<Pole> getAll() {
        return poles;
    }
}
