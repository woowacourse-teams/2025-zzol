package coffeeshout.wormgame.domain;

import java.util.List;

/** 풀 스냅샷 한 줄 — 재접속 복구용 궤적(간격 샘플링). */
public record WormTrailSnapshot(String playerName, boolean alive, List<Point> trail) {

    static WormTrailSnapshot of(Worm worm, int stride) {
        return new WormTrailSnapshot(
                worm.getGamer().getName(), worm.isAlive(), worm.getTrail().sampled(stride));
    }
}
