package coffeeshout.laddergame.domain;

import coffeeshout.global.exception.custom.BusinessException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LadderLines {

    public static final int MAX_LINES_PER_PLAYER = 3;

    private final List<LadderLine> lines = new ArrayList<>();

    // 개수·자리 검사와 추가를 한 임계 구역에 둔다. 나누면 동시 요청 둘이 같은 자리를 함께 통과한다.
    public synchronized LadderLine add(String playerName, int segmentIndex, int row) {
        if (countOf(playerName) >= MAX_LINES_PER_PLAYER) {
            throw new BusinessException(LadderGameErrorCode.LINE_LIMIT_EXCEEDED, "선을 모두 그은 플레이어입니다: " + playerName);
        }
        if (isOccupied(segmentIndex, row)) {
            throw new BusinessException(
                    LadderGameErrorCode.ROW_OCCUPIED, "이미 선이 있는 자리입니다: segment=" + segmentIndex + ", row=" + row);
        }
        final LadderLine line = new LadderLine(playerName, segmentIndex, row);
        lines.add(line);
        return line;
    }

    public synchronized int countOf(String playerName) {
        return (int)
                lines.stream().filter(l -> l.playerName().equals(playerName)).count();
    }

    // 같은 높이에서 기둥을 공유하는 구간(같은 구간, 좌우 옆 구간)에 선이 있으면 막힌다
    public synchronized boolean isOccupied(int segmentIndex, int row) {
        return lines.stream().anyMatch(l -> l.row() == row && Math.abs(l.segmentIndex() - segmentIndex) <= 1);
    }

    public synchronized int trace(int startPoleIndex) {
        final List<LadderLine> sorted =
                lines.stream().sorted(Comparator.comparingInt(LadderLine::row)).toList();
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

    public synchronized int size() {
        return lines.size();
    }

    public synchronized List<LadderLine> getAll() {
        return List.copyOf(lines);
    }
}
