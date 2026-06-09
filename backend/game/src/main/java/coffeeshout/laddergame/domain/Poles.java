package coffeeshout.laddergame.domain;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.global.exception.custom.BusinessException;
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

    public int getPoleIndex(String playerName) {
        return poles.stream()
                .filter(p -> p.gamer().name().equals(playerName))
                .mapToInt(Pole::index)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        LadderGameErrorCode.PLAYER_NOT_FOUND,
                        "플레이어를 찾을 수 없습니다: " + playerName
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

    public Gamer findGamer(String playerName) {
        return poles.stream()
                .map(Pole::gamer)
                .filter(gamer -> gamer.name().equals(playerName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        LadderGameErrorCode.PLAYER_NOT_FOUND,
                        "플레이어를 찾을 수 없습니다: " + playerName
                ));
    }

    public int size() {
        return poles.size();
    }

    public boolean isValidSegment(int segmentIndex) {
        return segmentIndex >= 0 && segmentIndex <= size() - 2;
    }

    public boolean contains(String playerName) {
        return poles.stream().anyMatch(p -> p.gamer().name().equals(playerName));
    }

    public List<Pole> getAll() {
        return poles;
    }
}
