package coffeeshout.laddergame.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LadderGame implements Playable {

    private volatile LadderGameState state;
    private Poles poles;
    private LadderLines lines = new LadderLines();
    private BottomRanks bottomRanks;
    private Map<Player, Integer> finalRanks;

    public LadderGame() {
        this.state = LadderGameState.DESCRIPTION;
    }

    public LadderGameState getState() {
        return state;
    }

    public Poles getPoles() {
        return poles;
    }

    public LadderLines getLines() {
        return lines;
    }

    public BottomRanks getBottomRanks() {
        return bottomRanks;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        this.state = LadderGameState.DESCRIPTION;
        this.lines = new LadderLines();
        final List<Player> playerList = gamers.stream()
                .map(g -> Player.createGuest(new PlayerName(g.name()), g.userId()))
                .toList();
        this.poles = Poles.assign(playerList);
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
            throw new BusinessException(LadderGameErrorCode.INVALID_STATE_TRANSITION,
                    state + " → " + next + " 전환은 허용되지 않습니다.");
        }
        this.state = next;
    }

    public LadderLine drawLine(PlayerName playerName, int segmentIndex) {
        poles.getPoleIndex(playerName);
        if (lines.hasDrawn(playerName)) {
            throw new BusinessException(LadderGameErrorCode.ALREADY_DREW,
                    "이미 선을 그은 플레이어입니다: " + playerName.value());
        }
        return lines.add(playerName, segmentIndex);
    }

    public boolean isAlreadyDrew(PlayerName playerName) {
        return lines.hasDrawn(playerName);
    }

    public void tracePaths() {
        final Map<Player, Integer> ranks = new HashMap<>();
        for (int i = 0; i < poles.size(); i++) {
            final Player player = poles.getPlayer(i);
            final int finalPoleIndex = lines.trace(i);
            ranks.put(player, bottomRanks.getRank(finalPoleIndex));
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
                        e -> e.getKey().getName().value(),
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
                        e -> e.getKey().toGamer(),
                        e -> new LadderGameScore(e.getValue())
                ));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.LADDER_GAME;
    }
}
