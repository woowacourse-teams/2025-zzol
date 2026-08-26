package coffeeshout.wormgame.domain;

/** 틱 델타 한 줄 — 머리 위치·방향·생존 여부·서버가 마지막으로 적용한 조향 seq(클라 예측 보정 기준). */
public record WormPosition(String playerName, double x, double y, double angle, boolean alive, long lastSeq) {

    static WormPosition of(Worm worm) {
        return new WormPosition(
                worm.getGamer().getName(),
                worm.getX(),
                worm.getY(),
                worm.getAngle(),
                worm.isAlive(),
                worm.getLastSeq());
    }
}
