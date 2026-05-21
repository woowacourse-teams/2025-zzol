package coffeeshout.minigame.ui.response;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.minigame.domain.MiniGameScore;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public record MiniGameScoresResponse(List<MiniGameScoreResponse> scores) {

    public record MiniGameScoreResponse(
            String playerName,
            Long score
    ) {

        public static MiniGameScoreResponse from(@NonNull Map.Entry<Gamer, MiniGameScore> scoreEntry) {
            return new MiniGameScoreResponse(
                    scoreEntry.getKey().name(),
                    scoreEntry.getValue().getValue()
            );
        }
    }

    public static MiniGameScoresResponse from(Map<Gamer, MiniGameScore> miniGameScores) {
        return new MiniGameScoresResponse(
                miniGameScores.entrySet().stream()
                        .map(MiniGameScoreResponse::from)
                        .toList());
    }
}
