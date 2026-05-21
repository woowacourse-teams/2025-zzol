package coffeeshout.laddergame.domain;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.Playable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;


@Getter
public class LadderGame implements Playable {

    private volatile LadderGameState state;
    private Poles poles;
    private LadderLines lines = new LadderLines();
    private BottomRanks bottomRanks;
    private Map<Gamer, Integer> finalRanks;
    private List<Gamer> gamers;

    public LadderGame() {
        this.state = LadderGameState.DESCRIPTION;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        this.gamers = List.copyOf(gamers);
        this.state = LadderGameState.DESCRIPTION;
        this.lines = new LadderLines();
        this.poles = Poles.assign(gamers);
        this.bottomRanks = BottomRanks.generate(gamers.size());
        this.finalRanks = null;
    }

    @Override
    public List<Gamer> getGamers() {
        return gamers;
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
            throw new BusinessException(LadderGameErrorCode.INVALID_STATE_TRANSITION,
                    state + " → " + next + " 전환은 허용되지 않습니다.");
        }
        this.state = next;
    }

    public LadderLine drawLine(Gamer gamer, int segmentIndex) {
        gamer.validateAgainst(this.gamers);
        poles.getPoleIndex(gamer);
        if (lines.hasDrawn(gamer)) {
            throw new BusinessException(LadderGameErrorCode.ALREADY_DREW,
                    "이미 선을 그은 플레이어입니다: " + gamer.name().value());
        }
        return lines.add(gamer, segmentIndex);
    }

    public boolean isAlreadyDrew(Gamer gamer) {
        return lines.hasDrawn(gamer);
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
            throw new BusinessException(LadderGameErrorCode.PATH_NOT_TRACED,
                    "tracePaths()가 먼저 호출되어야 합니다.");
        }
        return finalRanks.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name().value(),
                        Map.Entry::getValue
                ));
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromAscending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        if (finalRanks == null) {
            throw new BusinessException(LadderGameErrorCode.PATH_NOT_TRACED,
                    "tracePaths()가 먼저 호출되어야 합니다.");
        }
        return finalRanks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new LadderGameScore(e.getValue())
                ));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.LADDER_GAME;
    }
}
