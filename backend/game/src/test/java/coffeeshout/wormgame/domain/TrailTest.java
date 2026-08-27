package coffeeshout.wormgame.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class TrailTest {

    private Trail trailOf(int pointCount) {
        final Trail trail = new Trail();
        for (int i = 0; i < pointCount; i++) {
            trail.add(i, 0);
        }
        return trail;
    }

    @Test
    void 샘플링은_간격대로_뽑되_머리는_항상_포함한다() {
        // given — 6점, 간격 2면 0·2·4에 마지막 5가 더 붙는다
        final Trail trail = trailOf(6);

        // when
        final var sampled = trail.sampled(2);

        // then
        assertThat(sampled).extracting(Point::x).containsExactly(0.0, 2.0, 4.0, 5.0);
    }

    @Test
    void 마지막_점이_간격에_걸리면_중복하지_않는다() {
        // given — 5점, 간격 2면 0·2·4로 끝난다
        final Trail trail = trailOf(5);

        // when
        final var sampled = trail.sampled(2);

        // then
        assertThat(sampled).extracting(Point::x).containsExactly(0.0, 2.0, 4.0);
    }

    @Test
    void 점이_하나면_세그먼트는_없다() {
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(trailOf(1).segmentCount()).isZero();
        softly.assertThat(trailOf(0).sampled(2)).isEmpty();
        softly.assertAll();
    }
}
