package coffeeshout.wormgame.domain;

/**
 * 선분-선분 최소 거리. 충돌 판정은 점이 아니라 선분(swept)이다 — 최고 속도에서 틱당 이동량이
 * 판정 반경을 넘어 점 대 점 비교는 관통(터널링)을 놓친다(설계 문서 v0.3).
 */
final class SegmentGeometry {

    private SegmentGeometry() {}

    static double distance(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        if (intersects(ax, ay, bx, by, cx, cy, dx, dy)) {
            return 0;
        }
        return Math.min(
                Math.min(pointToSegment(ax, ay, cx, cy, dx, dy), pointToSegment(bx, by, cx, cy, dx, dy)),
                Math.min(pointToSegment(cx, cy, ax, ay, bx, by), pointToSegment(dx, dy, ax, ay, bx, by)));
    }

    static double pointToSegment(double px, double py, double ax, double ay, double bx, double by) {
        final double abx = bx - ax;
        final double aby = by - ay;
        final double lengthSquared = abx * abx + aby * aby;
        double t = 0;
        if (lengthSquared > 0) {
            t = Math.clamp(((px - ax) * abx + (py - ay) * aby) / lengthSquared, 0.0, 1.0);
        }
        final double qx = ax + t * abx;
        final double qy = ay + t * aby;
        return Math.hypot(px - qx, py - qy);
    }

    private static boolean intersects(
            double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        final double d1 = cross(cx, cy, dx, dy, ax, ay);
        final double d2 = cross(cx, cy, dx, dy, bx, by);
        final double d3 = cross(ax, ay, bx, by, cx, cy);
        final double d4 = cross(ax, ay, bx, by, dx, dy);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double cross(double ax, double ay, double bx, double by, double px, double py) {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }
}
