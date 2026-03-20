package coffeeshout.room.domain.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ColorUsageTest {

    private ColorUsage colorUsage;

    @BeforeEach
    void setUp() {
        colorUsage = new ColorUsage("ABC23");
    }

    @Test
    void pickRandomOne은_0부터8사이값을_반환한다() {
        // when
        int colorIndex = colorUsage.pickRandomOne();

        // then
        assertThat(colorIndex).isBetween(0, 8);
    }

    @Test
    void pickRandomOne은_중복된_색깔을_반환하지않는다() {
        // when
        int first = colorUsage.pickRandomOne();
        int second = colorUsage.pickRandomOne();

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @RepeatedTest(10)
    void pickRandomOne을_9번호출하면_모든색깔이_나온다() {
        // given
        Set<Integer> pickedColors = new HashSet<>();

        // when
        for (int i = 0; i < 9; i++) {
            int color = colorUsage.pickRandomOne();
            pickedColors.add(color);
        }

        // then
        assertThat(pickedColors).hasSize(9);
        assertThat(pickedColors).containsExactlyInAnyOrder(0, 1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    void 모든색깔을_사용후_pickRandomOne호출시_예외가발생한다() {
        // given
        for (int i = 0; i < 9; i++) {
            colorUsage.pickRandomOne();
        }

        // when & then
        assertThatThrownBy(() -> colorUsage.pickRandomOne())
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("사용가능한 색깔을 찾지 못했습니다");
    }

    @Test
    void release에_음수인덱스_전달시_예외가발생한다() {
        // when & then
        assertThatThrownBy(() -> colorUsage.release(-1))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("유효하지 않은 색깔 index입니다");
    }

    @Test
    void release에_9이상인덱스_전달시_예외가발생한다() {
        // when & then
        assertThatThrownBy(() -> colorUsage.release(9))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("유효하지 않은 색깔 index입니다");
    }

    @Test
    void 모든색깔사용후_release호출시_다시_pickRandomOne이_가능하다() {
        // given
        for (int i = 0; i < 9; i++) {
            colorUsage.pickRandomOne();
        }

        // when
        colorUsage.release(3);
        int newColor = colorUsage.pickRandomOne();

        // then
        assertThat(newColor).isEqualTo(3);
    }
}
