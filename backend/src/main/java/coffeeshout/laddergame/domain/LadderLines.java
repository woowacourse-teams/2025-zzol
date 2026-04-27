package coffeeshout.laddergame.domain;

import coffeeshout.room.domain.player.PlayerName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LadderLines {

    private final List<LadderLine> lines = new ArrayList<>();

    public synchronized LadderLine add(PlayerName playerName, int segmentIndex) {
        int candidate = lines.stream()
                .filter(l -> l.segmentIndex() == segmentIndex)
                .mapToInt(LadderLine::row)
                .max()
                .orElse(0) + 1;

        while (hasAdjacentLineAtRow(segmentIndex, candidate)) {
            candidate++;
        }

        final LadderLine line = new LadderLine(playerName, segmentIndex, candidate);
        lines.add(line);
        return line;
    }

    public boolean hasDrawn(PlayerName playerName) {
        return lines.stream().anyMatch(l -> l.playerName().equals(playerName));
    }

    public int trace(int startPoleIndex) {
        final List<LadderLine> sorted = lines.stream()
                .sorted(Comparator.comparingInt(LadderLine::row))
                .toList();
        int currentPole = startPoleIndex;
        for (LadderLine line : sorted) {
            if (line.segmentIndex() == currentPole) {
                currentPole++;
            } else if (line.segmentIndex() + 1 == currentPole) {
                currentPole--;
            }
        }
        return currentPole;
    }

    public int size() {
        return lines.size();
    }

    public List<LadderLine> getAll() {
        return List.copyOf(lines);
    }

    private boolean hasAdjacentLineAtRow(int segmentIndex, int row) {
        return lines.stream().anyMatch(l ->
                (l.segmentIndex() == segmentIndex - 1 || l.segmentIndex() == segmentIndex + 1)
                        && l.row() == row
        );
    }
}
