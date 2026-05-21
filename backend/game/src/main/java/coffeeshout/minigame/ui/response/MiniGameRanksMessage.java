package coffeeshout.minigame.ui.response;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.NonNull;

public record MiniGameRanksMessage(List<MiniGameRankMessage> ranks) {

    public record MiniGameRankMessage(
            String playerName,
            String userCode,
            Integer rank
    ) {

        public static MiniGameRankMessage from(
                @NonNull Entry<Gamer, Integer> rankEntry,
                @NonNull Map<Long, String> userCodes
        ) {
            final Gamer gamer = rankEntry.getKey();
            return new MiniGameRankMessage(
                    gamer.name().value(),
                    gamer.userId() != null ? userCodes.get(gamer.userId()) : null,
                    rankEntry.getValue()
            );
        }
    }

    public static MiniGameRanksMessage from(
            @NonNull MiniGameResult miniGameResult,
            @NonNull Map<Long, String> userCodes
    ) {
        final List<MiniGameRankMessage> message = miniGameResult.getRank().entrySet()
                .stream()
                .map(e -> MiniGameRankMessage.from(e, userCodes))
                .toList();
        return new MiniGameRanksMessage(message);
    }
}
