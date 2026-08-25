package coffeeshout.wormgame.domain.event;

import coffeeshout.wormgame.domain.WormGame;
import coffeeshout.wormgame.domain.WormTrailSnapshot;
import java.time.Instant;
import java.util.List;

/**
 * 풀 스냅샷 — 재접속 복구용. 시간의 진실은 tick이고 {@code serverNow}는 클라가 tick↔절대시각을 매핑하는
 * 용도로만 쓴다(벽시계로 판정하지 않는다). 스냅샷 tick 이전의 델타는 클라가 폐기한다.
 */
public record WormSnapshotEvent(
        String joinCode, long tick, long tickMillis, Instant serverNow, double radius, List<WormTrailSnapshot> worms) {

    /** 궤적 샘플링 간격(점 단위). 틱당 6~10u이므로 2점 간격 ≈ 12~20u — 복구 렌더에 충분하고 크기는 절반. */
    static final int TRAIL_STRIDE = 2;

    public static WormSnapshotEvent of(WormGame game, String joinCode) {
        return new WormSnapshotEvent(
                joinCode,
                game.getTickCount(),
                game.getRules().tickMillis(),
                Instant.now(),
                game.currentRadius(),
                game.trailSnapshots(TRAIL_STRIDE));
    }
}
