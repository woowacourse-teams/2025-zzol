package coffeeshout.laddergame.domain;

import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.player.Player;
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
    private final LadderLines lines = new LadderLines();
    private BottomRanks bottomRanks;
    private Map<Player, Integer> finalRanks;

    public LadderGame() {
        this.state = LadderGameState.DESCRIPTION;
    }

    @Override
    public void setUp(List<Player> players) {
        this.poles = Poles.assign(players);
        this.bottomRanks = BottomRanks.generate(players.size());
        this.finalRanks = null;
    }

    public void changeToDescription() {
        this.state = LadderGameState.DESCRIPTION;
    }

    public void changeToPrepare() {
        this.state = LadderGameState.PREPARE;
    }

    public void changeToDrawing() {
        this.state = LadderGameState.DRAWING;
    }

    public void changeToResult() {
        this.state = LadderGameState.RESULT;
    }

    public void changeToDone() {
        this.state = LadderGameState.DONE;
    }

    public LadderLine drawLine(PlayerName playerName, int segmentIndex) {
        poles.getPoleIndex(playerName); // validates player exists
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
    public Map<Player, MiniGameScore> getScores() {
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
