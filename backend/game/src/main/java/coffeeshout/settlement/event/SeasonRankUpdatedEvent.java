package coffeeshout.settlement.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 정산 완료 후 방에 알리는 순위 변동 이벤트. 정산 작업 큐와 달리 이 이벤트는 기존
 * <b>브로드캐스트 스트림</b>(minigame)으로 나간다 — 각 인스턴스가 자기 로컬 WebSocket
 * 세션에 알림을 뿌려야 하기 때문이다. 알림은 유실 허용이므로 eventId도 랜덤이면 충분하다(#1610).
 */
public record SeasonRankUpdatedEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String seasonKey,
        List<RankEntry> entries
) implements BaseEvent {

    public SeasonRankUpdatedEvent {
        entries = List.copyOf(entries);
    }

    public static SeasonRankUpdatedEvent of(String joinCode, String seasonKey, List<RankEntry> entries) {
        return new SeasonRankUpdatedEvent(UUID.randomUUID().toString(), Instant.now(), joinCode, seasonKey, entries);
    }

    public record RankEntry(String playerName, long totalPoints, String tier, int seasonRank) {
    }
}
