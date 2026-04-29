package coffeeshout.laddergame.domain;

import coffeeshout.room.domain.player.PlayerName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LadderLines {

    private final List<LadderLine> lines = new ArrayList<>();

    public synchronized LadderLine add(PlayerName playerName, int segmentIndex) {
        final LadderLine line = new LadderLine(playerName, segmentIndex, lines.size() + 1);
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

}
