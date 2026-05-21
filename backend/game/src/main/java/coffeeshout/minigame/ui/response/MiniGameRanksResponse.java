package coffeeshout.minigame.ui.response;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public record MiniGameRanksResponse(List<MiniGameRankResponse> ranks) {

    public record MiniGameRankResponse(
            String playerName,
            String userCode,
            Integer rank
    ) {

        public static MiniGameRankResponse from(
                @NonNull Map.Entry<Gamer, Integer> rankEntry,
                @NonNull Map<Long, String> userCodes
        ) {
            final Gamer gamer = rankEntry.getKey();
            return new MiniGameRankResponse(
                    gamer.name().value(),
                    gamer.userId() != null ? userCodes.get(gamer.userId()) : null,
                    rankEntry.getValue()
            );
        }
    }

    public static MiniGameRanksResponse from(
            @NonNull MiniGameResult miniGameResult,
            @NonNull Map<Long, String> userCodes
    ) {
        return new MiniGameRanksResponse(
                miniGameResult.getRank().entrySet().stream()
                        .map(e -> MiniGameRankResponse.from(e, userCodes))
                        .toList());
    }
}
