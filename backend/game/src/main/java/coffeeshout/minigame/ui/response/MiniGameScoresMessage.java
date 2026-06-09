package coffeeshout.minigame.ui.response;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.minigame.domain.MiniGameScore;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public record MiniGameScoresMessage(List<MiniGameScoreMessage> scores) {

    public record MiniGameScoreMessage(
            String playerName,
            Long score
    ) {

        public static MiniGameScoreMessage from(@NonNull Map.Entry<Gamer, MiniGameScore> scoreEntry) {
            return new MiniGameScoreMessage(scoreEntry.getKey().name(), scoreEntry.getValue().getValue());
        }
    }

    public static MiniGameScoresMessage from(Map<Gamer, MiniGameScore> miniGameScores) {
        return new MiniGameScoresMessage(
                miniGameScores.entrySet().stream()
                        .map(MiniGameScoreMessage::from)
                        .toList());
    }
}
