package coffeeshout.minigame.ui.response;

import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public record MiniGameScoresMessage(List<MiniGameScoreMessage> scores) {

    public record MiniGameScoreMessage(
            String playerName,
            Long score
    ) {

        public static MiniGameScoreMessage from(@NonNull Map.Entry<Player, MiniGameScore> scoreEntry) {
            return new MiniGameScoreMessage(scoreEntry.getKey().getName().value(), scoreEntry.getValue().getValue());
        }
    }

    public static MiniGameScoresMessage from(Map<Player, MiniGameScore> miniGameScores) {
        return new MiniGameScoresMessage(
                miniGameScores.entrySet().stream()
                        .map(MiniGameScoreMessage::from)
                        .toList());
    }
}
