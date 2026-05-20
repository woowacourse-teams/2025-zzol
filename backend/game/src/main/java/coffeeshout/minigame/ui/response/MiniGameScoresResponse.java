package coffeeshout.minigame.ui.response;

import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public record MiniGameScoresResponse(List<MiniGameScoreResponse> scores) {

    public record MiniGameScoreResponse(
            String playerName,
            Long score
    ) {

        public static MiniGameScoreResponse from(@NonNull Map.Entry<PlayerName, MiniGameScore> scoreEntry) {
            return new MiniGameScoreResponse(
                    scoreEntry.getKey().value(),
                    scoreEntry.getValue().getValue()
            );
        }
    }

    public static MiniGameScoresResponse from(Map<PlayerName, MiniGameScore> miniGameScores) {
        return new MiniGameScoresResponse(
                miniGameScores.entrySet().stream()
                        .map(MiniGameScoreResponse::from)
                        .toList());
    }
}
