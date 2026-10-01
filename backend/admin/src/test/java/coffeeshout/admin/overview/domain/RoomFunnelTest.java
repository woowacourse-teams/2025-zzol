package coffeeshout.admin.overview.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RoomFunnel")
class RoomFunnelTest {

    @Nested
    class completionRate {

        @Test
        void 생성_대비_완주_비율을_계산한다() {
            assertThat(new RoomFunnel(100, 80, 60, 40, 25).completionRate()).isEqualTo(0.25);
        }

        @Test
        void 생성이_0이면_0이다() {
            // 0으로 나누면 NaN 이 되고 JSON 직렬화가 깨진다.
            assertThat(RoomFunnel.empty().completionRate()).isZero();
        }

        @Test
        void 전부_완주하면_1이다() {
            assertThat(new RoomFunnel(10, 10, 10, 10, 10).completionRate()).isEqualTo(1.0);
        }
    }

    @Nested
    class empty {

        @Test
        void 모든_단계가_0이다() {
            final RoomFunnel funnel = RoomFunnel.empty();

            assertThat(funnel.created()).isZero();
            assertThat(funnel.miniGamePlayed()).isZero();
            assertThat(funnel.gameStarted()).isZero();
            assertThat(funnel.rouletteReached()).isZero();
            assertThat(funnel.completed()).isZero();
        }
    }
}
