package coffeeshout.laddergame.domain;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
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
    class add_테스트 {

        @Test
        void 요청한_row에_선이_그어진다() {
            final LadderLine line = lines.add(꾹이, 2, 5);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(line.playerName()).isEqualTo(꾹이);
                softly.assertThat(line.segmentIndex()).isEqualTo(2);
                softly.assertThat(line.row()).isEqualTo(5);
            });
        }

        @Test
        void 한_플레이어는_선을_2개까지_그을_수_있다() {
            lines.add(꾹이, 0, 1);
            lines.add(꾹이, 0, 2);

            assertThat(lines.countOf(꾹이)).isEqualTo(2);
        }

        @Test
        void 세_번째_선을_그으면_예외를_던진다() {
            lines.add(꾹이, 0, 1);
            lines.add(꾹이, 0, 2);

            assertCoffeeShoutException(() -> lines.add(꾹이, 0, 3), LadderGameErrorCode.LINE_LIMIT_EXCEEDED);
        }

        @Test
        void 다른_플레이어의_선은_내_개수에_포함되지_않는다() {
            lines.add(꾹이, 0, 1);
            lines.add(꾹이, 0, 2);

            final LadderLine line = lines.add(철수, 0, 3);

            assertThat(line.playerName()).isEqualTo(철수);
        }

        @Test
        void 이미_선이_있는_자리에_그으면_예외를_던진다() {
            lines.add(꾹이, 1, 4);

            assertCoffeeShoutException(() -> lines.add(철수, 1, 4), LadderGameErrorCode.ROW_OCCUPIED);
        }

        @Test
        void 거절된_선은_개수에_포함되지_않는다() {
            lines.add(꾹이, 1, 4);

            assertCoffeeShoutException(() -> lines.add(철수, 1, 4), LadderGameErrorCode.ROW_OCCUPIED);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(lines.countOf(철수)).isZero();
                softly.assertThat(lines.size()).isEqualTo(1);
            });
        }
    }

    @Nested
    class isOccupied_테스트 {

        @BeforeEach
        void 구간1_높이4에_선을_긋는다() {
            lines.add(꾹이, 1, 4);
        }

        @Test
        void 같은_높이_같은_칸은_막혀_있다() {
            assertThat(lines.isOccupied(1, 4)).isTrue();
        }

        @Test
        void 같은_높이_왼쪽_옆_칸은_기둥을_공유하므로_막혀_있다() {
            assertThat(lines.isOccupied(0, 4)).isTrue();
        }

        @Test
        void 같은_높이_오른쪽_옆_칸은_기둥을_공유하므로_막혀_있다() {
            assertThat(lines.isOccupied(2, 4)).isTrue();
        }

        @Test
        void 같은_높이라도_기둥을_공유하지_않으면_비어_있다() {
            assertThat(lines.isOccupied(3, 4)).isFalse();
        }

        @Test
        void 같은_칸이라도_높이가_다르면_비어_있다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(lines.isOccupied(1, 3)).isFalse();
                softly.assertThat(lines.isOccupied(1, 5)).isFalse();
            });
        }
    }

    @Nested
    class countOf_테스트 {

        @Test
        void 선을_긋지_않은_플레이어는_0개다() {
            assertThat(lines.countOf(꾹이)).isZero();
        }

        @Test
        void 플레이어별로_따로_센다() {
            lines.add(꾹이, 0, 1);
            lines.add(꾹이, 0, 2);
            lines.add(철수, 2, 1);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(lines.countOf(꾹이)).isEqualTo(2);
                softly.assertThat(lines.countOf(철수)).isEqualTo(1);
                softly.assertThat(lines.countOf(영희)).isZero();
            });
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
            lines.add(꾹이, 1, 1);

            assertThat(lines.trace(1)).isEqualTo(2);
        }

        @Test
        void 현재_기둥이_선의_오른쪽_끝이면_왼쪽으로_이동한다() {
            // 구간 1(기둥 1-2 연결), 기둥 2에서 출발 → 기둥 1로
            lines.add(꾹이, 1, 1);

            assertThat(lines.trace(2)).isEqualTo(1);
        }

        @Test
        void 관계없는_구간의_선은_경로에_영향을_주지_않는다() {
            // 기둥 0에서 출발, 구간 2(기둥 2-3)의 선은 영향 없음
            lines.add(꾹이, 2, 1);

            assertThat(lines.trace(0)).isEqualTo(0);
        }

        @Test
        void 그은_순서가_아니라_row_순서로_따라간다() {
            lines.add(꾹이, 1, 5); // 먼저 그었지만 아래(row=5)
            lines.add(철수, 0, 2); // 나중에 그었지만 위(row=2)

            // row 순: row=2 구간0 → 기둥1, row=5 구간1 → 기둥2
            // 그은 순서대로 따라가면 구간1을 먼저 만나 영향이 없고 구간0에서 기둥1로 끝난다
            assertThat(lines.trace(0)).isEqualTo(2);
        }

        @Test
        void 같은_높이의_떨어진_선은_각자_경로에만_영향을_준다() {
            lines.add(꾹이, 0, 3);
            lines.add(철수, 2, 3);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(lines.trace(0)).isEqualTo(1);
                softly.assertThat(lines.trace(1)).isEqualTo(0);
                softly.assertThat(lines.trace(2)).isEqualTo(3);
                softly.assertThat(lines.trace(3)).isEqualTo(2);
            });
        }

        @Test
        void 여러_선을_row_순서대로_따라가며_올바른_경로를_계산한다() {
            lines.add(꾹이, 0, 1);
            lines.add(영희, 0, 2);
            lines.add(철수, 1, 3);

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
        void getAll은_추가된_모든_선을_반환한다() {
            lines.add(꾹이, 0, 1);
            lines.add(철수, 2, 1);

            assertThat(lines.getAll()).hasSize(2);
        }
    }
}
