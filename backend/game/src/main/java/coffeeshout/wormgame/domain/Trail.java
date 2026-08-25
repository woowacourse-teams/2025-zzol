package coffeeshout.wormgame.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * append-only 궤적. 틱마다 머리 좌표가 하나씩 쌓이며 게임 중 절대 줄지 않는다(Tron식 영구 궤적).
 */
public class Trail {

    private final List<double[]> points = new ArrayList<>();

    void add(double x, double y) {
        points.add(new double[] {x, y});
    }

    public int pointCount() {
        return points.size();
    }

    public int segmentCount() {
        return Math.max(0, points.size() - 1);
    }

    double startX(int segmentIndex) {
        return points.get(segmentIndex)[0];
    }

    double startY(int segmentIndex) {
        return points.get(segmentIndex)[1];
    }

    double endX(int segmentIndex) {
        return points.get(segmentIndex + 1)[0];
    }

    double endY(int segmentIndex) {
        return points.get(segmentIndex + 1)[1];
    }

    public List<double[]> points() {
        return List.copyOf(points);
    }

    /** 테스트 전용 — 시나리오 궤적을 스폰 포인트 없이 구성하기 위한 초기화. */
    void clear() {
        points.clear();
    }
}
