package coffeeshout.settlement.ui.response;

import coffeeshout.settlement.event.SeasonRankUpdatedEvent;
import java.util.List;

/** 방 토픽으로 나가는 순위 변동 메시지. */
public record SeasonRankMessage(String seasonKey, List<Entry> entries) {

    public static SeasonRankMessage from(SeasonRankUpdatedEvent event) {
        final List<Entry> entries = event.entries().stream()
                .map(entry -> new Entry(entry.playerName(), entry.totalPoints(), entry.tier(), entry.seasonRank()))
                .toList();
        return new SeasonRankMessage(event.seasonKey(), entries);
    }

    public record Entry(String playerName, long totalPoints, String tier, int seasonRank) {
    }
}
