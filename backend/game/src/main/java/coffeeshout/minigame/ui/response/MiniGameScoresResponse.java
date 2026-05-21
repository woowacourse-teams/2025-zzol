package coffeeshout.minigame.ui.response;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameScore;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public record MiniGameScoresResponse(List<MiniGameScoreResponse> scores) {

    public record MiniGameScoreResponse(
            String playerName,
            String userCode,
            Long score
    ) {

        public static MiniGameScoreResponse from(
                @NonNull Map.Entry<Gamer, MiniGameScore> scoreEntry,
                @NonNull Map<Long, String> userCodes
        ) {
            final Gamer gamer = scoreEntry.getKey();
            return new MiniGameScoreResponse(
                    gamer.name().value(),
                    gamer.userId() != null ? userCodes.get(gamer.userId()) : null,
                    scoreEntry.getValue().getValue()
            );
        }
    }

    public static MiniGameScoresResponse from(
            @NonNull Map<Gamer, MiniGameScore> scores,
            @NonNull Map<Long, String> userCodes
    ) {
        return new MiniGameScoresResponse(
                scores.entrySet().stream()
                        .map(e -> MiniGameScoreResponse.from(e, userCodes))
                        .toList());
    }
}
