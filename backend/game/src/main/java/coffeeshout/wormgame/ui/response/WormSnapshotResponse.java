package coffeeshout.wormgame.ui.response;

import coffeeshout.wormgame.domain.WormTrailSnapshot;
import coffeeshout.wormgame.domain.event.WormSnapshotEvent;
import java.time.Instant;
import java.util.List;

/** 풀 스냅샷 — 스냅샷 tick 이전의 델타는 폐기. serverNow는 tick↔절대시각 매핑 전용. */
public record WormSnapshotResponse(
        long tick, long tickMillis, Instant serverNow, double radius, List<WormTrailSnapshot> worms) {

    public static WormSnapshotResponse from(WormSnapshotEvent event) {
        return new WormSnapshotResponse(
                event.tick(), event.tickMillis(), event.serverNow(), event.radius(), event.worms());
    }
}
