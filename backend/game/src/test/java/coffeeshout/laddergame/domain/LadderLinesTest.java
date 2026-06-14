package coffeeshout.laddergame.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LadderLinesTest {

    static final String 꾹이 = "꾹이";
    static final String 철수 = "철수";
    static final String 영희 = "영희";

    LadderLines lines;

    @BeforeEach
    void setUp() {
        lines = new LadderLines();
    }

    @Nested
    class add_row_계산_테스트 {

        @Test
        void 빈_상태에서_첫_선은_row_1을_받는다() {
            final LadderLine line = lines.add(꾹이, 0);

            assertThat(line.row()).isEqualTo(1);
        }

        @Test
        void 같은_구간에_두_번째_선은_기존_선보다_아래에_배치된다() {
            lines.add(꾹이, 1);
            final LadderLine second = lines.add(철수, 1);

            assertThat(second.row()).isEqualTo(2);
        }

        @Test
        void 같은_구간에_세_번째_선은_row_3을_받는다() {
            lines.add(꾹이, 0);
            lines.add(철수, 0);
            final LadderLine third = lines.add(영희, 0);

            assertThat(third.row()).isEqualTo(3);
        }

        @Test
        void 서로_다른_구간을_눌러도_누른_순서대로_row가_증가한다() {
            final LadderLine first = lines.add(꾹이, 0);
            final LadderLine second = lines.add(철수, 2);
            final LadderLine third = lines.add(영희, 1);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(first.row()).isEqualTo(1);
                softly.assertThat(second.row()).isEqualTo(2);
                softly.assertThat(third.row()).isEqualTo(3);
            });
        }

        @Test
        void 반환된_LadderLine에_playerName과_segmentIndex가_포함된다() {
            final LadderLine line = lines.add(꾹이, 2);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(line.playerName()).isEqualTo(꾹이);
                softly.assertThat(line.segmentIndex()).isEqualTo(2);
            });
        }
    }

    @Nested
    class hasDrawn_테스트 {

        @Test
        void 선을_그은_플레이어는_true를_반환한다() {
            lines.add(꾹이, 0);

            assertThat(lines.hasDrawn(꾹이)).isTrue();
        }

        @Test
        void 선을_그지_않은_플레이어는_false를_반환한다() {
            lines.add(꾹이, 0);

            assertThat(lines.hasDrawn(철수)).isFalse();
        }

        @Test
        void 빈_상태에서는_항상_false를_반환한다() {
            assertThat(lines.hasDrawn(꾹이)).isFalse();
        }
    }

    @Nested
    class trace_테스트 {

        @Test
        void 선이_없으면_시작_기둥을_그대로_반환한다() {
            assertThat(lines.trace(0)).isEqualTo(0);
            assertThat(lines.trace(2)).isEqualTo(2);
        }

        @Test
        void 현재_기둥이_선의_왼쪽_끝이면_오른쪽으로_이동한다() {
            // 구간 1(기둥 1-2 연결), 기둥 1에서 출발 → 기둥 2로
            lines.add(꾹이, 1);

            assertThat(lines.trace(1)).isEqualTo(2);
        }

        @Test
        void 현재_기둥이_선의_오른쪽_끝이면_왼쪽으로_이동한다() {
            // 구간 1(기둥 1-2 연결), 기둥 2에서 출발 → 기둥 1로
            lines.add(꾹이, 1);

            assertThat(lines.trace(2)).isEqualTo(1);
        }

        @Test
        void 관계없는_구간의_선은_경로에_영향을_주지_않는다() {
            // 기둥 0에서 출발, 구간 2(기둥 2-3)의 선은 영향 없음
            lines.add(꾹이, 2);

            assertThat(lines.trace(0)).isEqualTo(0);
        }

        @Test
        void 여러_선을_row_순서대로_따라간다() {
            lines.add(꾹이, 0);   // 1번째 → row=1, 구간0
            lines.add(철수, 2);   // 2번째 → row=2, 구간2

            // 기둥0: row=1 구간0 만남→기둥1 이동, row=2 구간2는 관계없음→기둥1 유지
            assertThat(lines.trace(0)).isEqualTo(1);
        }

        @Test
        void 여러_구간의_선을_순서대로_따라가며_올바른_경로를_계산한다() {
            lines.add(꾹이, 0);   // 1번째 → row=1, 구간0
            lines.add(영희, 0);   // 2번째 → row=2, 구간0
            lines.add(철수, 1);   // 3번째 → row=3, 구간1

            // 기둥1: row=1 구간0(seg+1=1) → 기둥0, row=2 구간0(seg=0) → 기둥1, row=3 구간1(seg=1) → 기둥2
            assertThat(lines.trace(1)).isEqualTo(2);
        }
    }

    @Nested
    class size_및_getAll_테스트 {

        @Test
        void 빈_상태에서_size는_0이다() {
            assertThat(lines.size()).isZero();
        }

        @Test
        void 선_추가_후_size가_증가한다() {
            lines.add(꾹이, 0);
            lines.add(철수, 2);

            assertThat(lines.size()).isEqualTo(2);
        }

        @Test
        void getAll은_추가된_모든_선을_반환한다() {
            lines.add(꾹이, 0);
            lines.add(철수, 2);

            assertThat(lines.getAll()).hasSize(2);
        }
    }
}
