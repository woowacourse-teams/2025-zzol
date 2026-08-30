package coffeeshout.wormgame.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * append-only 궤적. 틱마다 머리 좌표가 하나씩 쌓이며 게임 중 절대 줄지 않는다(Tron식 영구 궤적).
 */
public class Trail {

    private final List<Point> points = new ArrayList<>();

    void add(double x, double y) {
        points.add(new Point(x, y));
    }

    public int pointCount() {
        return points.size();
    }

    public int segmentCount() {
        return Math.max(0, points.size() - 1);
    }

    Point start(int segmentIndex) {
        return points.get(segmentIndex);
    }

    Point end(int segmentIndex) {
        return points.get(segmentIndex + 1);
    }

    public List<Point> points() {
        return List.copyOf(points);
    }

    /** 스냅샷용 간격 샘플링 — 마지막 점(현재 머리)은 항상 포함한다. */
    public List<Point> sampled(int stride) {
        final List<Point> sampled = new ArrayList<>();
        for (int i = 0; i < points.size(); i += stride) {
            sampled.add(points.get(i));
        }
        if (!points.isEmpty() && (points.size() - 1) % stride != 0) {
            sampled.add(points.getLast());
        }
        return sampled;
    }

    /** 테스트 전용 — 시나리오 궤적을 스폰 포인트 없이 구성하기 위한 초기화. */
    void clear() {
        points.clear();
    }
}
