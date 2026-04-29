package coffeeshout.laddergame.domain;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BottomRanksTest {

    @Nested
    class generate_테스트 {

        @Test
        void n개의_기둥에_순위가_배정된다() {
            final BottomRanks ranks = BottomRanks.generate(4);

            assertThat(ranks.getAll()).hasSize(4);
        }

        @Test
        void 순위는_1부터_n까지다() {
            final BottomRanks ranks = BottomRanks.generate(3);
            final Collection<Integer> values = ranks.getAll().values();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(values).allMatch(r -> r >= 1 && r <= 3);
                softly.assertThat(values).doesNotHaveDuplicates();
                softly.assertThat(values).containsExactlyInAnyOrder(1, 2, 3);
            });
        }

        @Test
        void 기둥_인덱스는_0부터_n_minus_1까지다() {
            final BottomRanks ranks = BottomRanks.generate(3);

            assertThat(ranks.getAll().keySet()).containsExactlyInAnyOrder(0, 1, 2);
        }
    }

    @Nested
    class getRank_테스트 {

        @Test
        void 기둥_인덱스로_순위를_조회한다() {
            final BottomRanks ranks = BottomRanks.generate(3);

            final int rank = ranks.getRank(0);

            assertThat(rank).isBetween(1, 3);
        }

        @Test
        void 존재하지_않는_기둥_인덱스는_예외를_던진다() {
            final BottomRanks ranks = BottomRanks.generate(3);

            assertCoffeeShoutException(
                    () -> ranks.getRank(99),
                    LadderGameErrorCode.INVALID_POLE_INDEX
            );
        }
    }
}
