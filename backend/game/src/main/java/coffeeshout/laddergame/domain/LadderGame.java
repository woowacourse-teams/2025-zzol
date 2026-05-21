package coffeeshout.laddergame.domain;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.Playable;
import coffeeshout.room.domain.player.PlayerName;
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
    private Map<PlayerName, Integer> finalRanks;
    private List<Gamer> gamers;

    public LadderGame() {
        this.state = LadderGameState.DESCRIPTION;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        this.gamers = List.copyOf(gamers);
        this.state = LadderGameState.DESCRIPTION;
        this.lines = new LadderLines();
        final List<PlayerName> playerNames = gamers.stream().map(Gamer::name).toList();
        this.poles = Poles.assign(playerNames);
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
        poles.getPoleIndex(gamer.name());
        if (lines.hasDrawn(gamer.name())) {
            throw new BusinessException(LadderGameErrorCode.ALREADY_DREW,
                    "이미 선을 그은 플레이어입니다: " + gamer.name().value());
        }
        return lines.add(gamer.name(), segmentIndex);
    }

    public boolean isAlreadyDrew(PlayerName playerName) {
        return lines.hasDrawn(playerName);
    }

    public void tracePaths() {
        final Map<PlayerName, Integer> ranks = new HashMap<>();
        for (int i = 0; i < poles.size(); i++) {
            final PlayerName playerName = poles.getPlayerName(i);
            final int finalPoleIndex = lines.trace(i);
            ranks.put(playerName, bottomRanks.getRank(finalPoleIndex));
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
                        e -> e.getKey().value(),
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
        final Map<PlayerName, Gamer> nameToGamer = gamers.stream()
                .collect(Collectors.toMap(Gamer::name, g -> g));
        return finalRanks.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> nameToGamer.get(e.getKey()),
                        e -> new LadderGameScore(e.getValue())
                ));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.LADDER_GAME;
    }
}
