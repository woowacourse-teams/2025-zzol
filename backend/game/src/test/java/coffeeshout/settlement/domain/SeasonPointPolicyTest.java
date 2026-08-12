package coffeeshout.settlement.domain;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.exception.GlobalErrorCode;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SeasonPointPolicyTest {

    @Nested
    class 동점이_없을_때 {

        @ParameterizedTest
        @CsvSource({"1, 100", "2, 70", "3, 50", "4, 30", "8, 30"})
        void 순위에_따라_기본_포인트를_환산한다(int rank, int expectedPoints) {
            List<Integer> allRanks = List.of(1, 2, 3, 4, 5, 6, 7, 8);

            assertThat(SeasonPointPolicy.pointsFor(rank, allRanks)).isEqualTo(expectedPoints);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void 순위가_1_미만이면_예외가_발생한다(int rank) {
            assertCoffeeShoutException(
                    () -> SeasonPointPolicy.pointsFor(rank, List.of(1, 2)), GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    class 동점이_있을_때 {

        @Test
        void 동점_구간의_포인트를_합산해_균등_분배한다() {
            // 2인 동점 1등 → 1·2위 포인트 합산 균등 분배: (100+70)/2 = 85
            List<Integer> allRanks = List.of(1, 1, 3);

            assertThat(SeasonPointPolicy.pointsFor(1, allRanks)).isEqualTo(85);
            assertThat(SeasonPointPolicy.pointsFor(3, allRanks)).isEqualTo(50);
        }

        @Test
        void 하위_순위_동점도_같은_방식으로_분배한다() {
            // 2인 동점 2등 → (70+50)/2 = 60
            List<Integer> allRanks = List.of(1, 2, 2, 4);

            assertThat(SeasonPointPolicy.pointsFor(2, allRanks)).isEqualTo(60);
        }

        @Test
        void 전원_동점이면_각자_평균_포인트를_받는다() {
            // 전원 실패로 전원 1등이 된 판 — (100+70+50+30)/4 = 62.5 → 반올림 63.
            // 정상 플레이의 기대 포인트와 같아 담합 이득이 없다
            List<Integer> allRanks = List.of(1, 1, 1, 1);

            assertThat(SeasonPointPolicy.pointsFor(1, allRanks)).isEqualTo(63);
        }

        @Test
        void 게스트가_낀_동점도_전체_인원_기준으로_분배한다() {
            // 회원 1명 + 게스트 1명이 동점 1등이어도 분배 기준은 전체 순위 분포다
            List<Integer> allRanks = List.of(1, 1, 3, 4);

            assertThat(SeasonPointPolicy.pointsFor(1, allRanks)).isEqualTo(85);
        }
    }

    @Nested
    class 순위_분포가_없을_때 {

        @Test
        void 구버전_이벤트는_기본_포인트로_처리한다() {
            // allRanks가 없는 재전달 이벤트 호환 — 동점 정보 없이 기본표를 적용한다
            assertThat(SeasonPointPolicy.pointsFor(1, List.of())).isEqualTo(100);
            assertThat(SeasonPointPolicy.pointsFor(2, null)).isEqualTo(70);
        }
    }
}
