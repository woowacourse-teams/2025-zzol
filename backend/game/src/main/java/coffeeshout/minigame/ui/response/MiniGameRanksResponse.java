package coffeeshout.minigame.ui.response;

import coffeeshout.gamecommon.PlayerView;
import coffeeshout.minigame.domain.MiniGameResult;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public record MiniGameRanksResponse(List<MiniGameRankResponse> ranks) {

    public record MiniGameRankResponse(
            String playerName,
            Integer rank
    ) {

        public static MiniGameRankResponse from(@NonNull Map.Entry<PlayerView, Integer> rankEntry) {
            return new MiniGameRankResponse(rankEntry.getKey().getNameValue(), rankEntry.getValue());
        }
    }

    public static MiniGameRanksResponse from(@NonNull MiniGameResult miniGameResult) {
        final List<MiniGameRankResponse> ranks = miniGameResult.getRank().entrySet()
                .stream()
                .map(MiniGameRankResponse::from)
                .toList();
        return new MiniGameRanksResponse(ranks);
    }
}
