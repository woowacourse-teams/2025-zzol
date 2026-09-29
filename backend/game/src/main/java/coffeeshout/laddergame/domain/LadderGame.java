package coffeeshout.laddergame.domain;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

public class LadderGame implements Playable {

    @Getter
    private volatile LadderGameState state;

    @Getter
    private Poles poles;

    @Getter
    private LadderLines lines = new LadderLines();

    @Getter
    private BottomRanks bottomRanks;

    private Map<Gamer, Integer> finalRanks;

    public LadderGame() {
        this.state = LadderGameState.DESCRIPTION;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        this.state = LadderGameState.DESCRIPTION;
        this.lines = new LadderLines();
        this.poles = Poles.assign(gamers);
        this.bottomRanks = BottomRanks.generate(gamers.size());
        this.finalRanks = null;
    }

    public void changeToPrepare() {
        transition(LadderGameState.PREPARE);
    }

    public void changeToDrawing() {
        transition(LadderGameState.DRAWING);
    }

    public void changeToResult() {
        transition(LadderGameState.RESULT);
    }

    public void changeToDone() {
        transition(LadderGameState.DONE);
    }

    private void transition(LadderGameState next) {
        if (!state.canTransitionTo(next)) {
            throw new BusinessException(
                    LadderGameErrorCode.INVALID_STATE_TRANSITION, state + " → " + next + " 전환은 허용되지 않습니다.");
        }
        this.state = next;
    }

    public LadderLine drawLine(String playerName, int segmentIndex, int row) {
        poles.getPoleIndex(playerName);
        if (!isValidRow(row)) {
            throw new BusinessException(LadderGameErrorCode.INVALID_LINE_ROW, "유효하지 않은 높이입니다: " + row);
        }
        return lines.add(playerName, segmentIndex, row);
    }

    public boolean canDraw(String playerName) {
        return lines.countOf(playerName) < LadderLines.MAX_LINES_PER_PLAYER;
    }

    // 모두가 한 구간에 최대 개수만큼 그어도 자리가 모자라지 않는 최솟값
    public int getRowCount() {
        return poles.size() * LadderLines.MAX_LINES_PER_PLAYER;
    }

    public boolean isValidRow(int row) {
        return row >= 1 && row <= getRowCount();
    }

    public boolean isOccupied(int segmentIndex, int row) {
        return lines.isOccupied(segmentIndex, row);
    }

    public void tracePaths() {
        final Map<Gamer, Integer> ranks = new HashMap<>();
        for (int i = 0; i < poles.size(); i++) {
            final Gamer gamer = poles.getGamer(i);
            final int finalPoleIndex = lines.trace(i);
            ranks.put(gamer, bottomRanks.getRank(finalPoleIndex));
        }
        this.finalRanks = Map.copyOf(ranks);
    }

    public Map<String, Integer> getRankingsForBroadcast() {
        if (finalRanks == null) {
            throw new BusinessException(LadderGameErrorCode.PATH_NOT_TRACED, "tracePaths()가 먼저 호출되어야 합니다.");
        }
        return finalRanks.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getName(), Map.Entry::getValue));
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromAscending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        if (finalRanks == null) {
            throw new BusinessException(LadderGameErrorCode.PATH_NOT_TRACED, "tracePaths()가 먼저 호출되어야 합니다.");
        }
        return finalRanks.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new LadderGameScore(e.getValue())));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.LADDER_GAME;
    }
}
