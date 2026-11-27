package coffeeshout.admin.quality.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NicknameAuditQuality")
class NicknameAuditQualityTest {

    @Test
    void 오탐과_미탐을_합쳐_뒤집힌_비율을_계산한다() {
        final NicknameAuditQuality quality = new NicknameAuditQuality(100, 10, 5, 40);

        assertThat(quality.overrideRate()).isEqualTo(0.15);
        assertThat(quality.agreed()).isEqualTo(85);
    }

    @Test
    void 판정이_없으면_0이다() {
        // 0으로 나누면 NaN 이 되고 JSON 직렬화가 깨진다.
        assertThat(NicknameAuditQuality.empty().overrideRate()).isZero();
    }

    @Test
    void 전부_뒤집히면_1이다() {
        assertThat(new NicknameAuditQuality(10, 6, 4, 4).overrideRate()).isEqualTo(1.0);
    }

    @Test
    void 전부_동의하면_0이다() {
        final NicknameAuditQuality quality = new NicknameAuditQuality(50, 0, 0, 0);

        assertThat(quality.overrideRate()).isZero();
        assertThat(quality.agreed()).isEqualTo(50);
    }

    @Nested
    class missUpperBound {

        @Test
        void 검토한_표본이_없으면_null이다() {
            // 0을 돌려주면 화면이 "미탐 0%"로 읽는다. 잰 적이 없다는 것과 구분해야 한다.
            assertThat(new NicknameAuditQuality(10, 2, 0, 0).missUpperBound()).isNull();
        }

        @Test
        void 미탐이_없으면_3의_법칙을_쓴다() {
            assertThat(new NicknameAuditQuality(100, 0, 0, 100).missUpperBound())
                    .isCloseTo(0.03, within(1e-9));
        }

        @Test
        void 표본이_3건_미만이면_상한은_1이다() {
            assertThat(new NicknameAuditQuality(2, 0, 0, 2).missUpperBound()).isEqualTo(1.0);
        }

        @Test
        void 미탐이_있으면_Wilson_구간의_위_끝이다() {
            // p = 0.05, n = 100, z = 1.96 을 손으로 풀면 0.11175 다.
            assertThat(new NicknameAuditQuality(100, 0, 5, 100).missUpperBound())
                    .isCloseTo(0.11175, within(1e-4));
        }

        @Test
        void 표본이_전부_미탐이면_1이다() {
            assertThat(new NicknameAuditQuality(20, 0, 20, 20).missUpperBound()).isCloseTo(1.0, within(1e-9));
        }
    }
}
