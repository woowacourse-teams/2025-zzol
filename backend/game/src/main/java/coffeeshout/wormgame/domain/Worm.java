package coffeeshout.wormgame.domain;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.global.exception.custom.BusinessException;
import lombok.Getter;

@Getter
public class Worm {

    private final Gamer gamer;
    private final Trail trail = new Trail();

    private double x;
    private double y;
    private double previousX;
    private double previousY;
    private double angle;
    /** 조향은 소비자 스레드가 쓰고 틱 스레드가 읽는다 — 마지막 값만 유효하므로 volatile로 충분하다. */
    private volatile double targetAngle;

    private volatile long lastSeq = -1;

    private boolean alive = true;
    private long deathTick = -1;

    Worm(Gamer gamer) {
        this.gamer = gamer;
    }

    void spawnAt(double x, double y, double heading) {
        placeAt(x, y, heading);
        trail.add(x, y);
    }

    void steer(double target, long seq) {
        if (!Double.isFinite(target)) {
            throw new BusinessException(WormGameErrorCode.INVALID_STEERING, "angle=" + target);
        }
        if (seq <= lastSeq) {
            return; // 스트림 순서 역전·중복 — 마지막 값만 유효
        }
        this.targetAngle = normalize(target);
        this.lastSeq = seq;
    }

    void advance(double distance, double maxTurn) {
        final double diff = normalize(targetAngle - angle);
        angle = normalize(angle + Math.clamp(diff, -maxTurn, maxTurn));
        previousX = x;
        previousY = y;
        x += Math.cos(angle) * distance;
        y += Math.sin(angle) * distance;
    }

    void appendHeadToTrail() {
        trail.add(x, y);
    }

    void die(long tick) {
        this.alive = false;
        this.deathTick = tick;
    }

    double distanceFromCenter() {
        return Math.hypot(x, y);
    }

    /** 테스트 전용 위치 고정. */
    void placeAt(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.previousX = x;
        this.previousY = y;
        this.angle = heading;
        this.targetAngle = heading;
    }

    private static double normalize(double radians) {
        return Math.atan2(Math.sin(radians), Math.cos(radians));
    }
}
