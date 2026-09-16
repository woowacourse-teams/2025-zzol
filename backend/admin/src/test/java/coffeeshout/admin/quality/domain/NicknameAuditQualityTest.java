package coffeeshout.admin.quality.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NicknameAuditQuality")
class NicknameAuditQualityTest {

    @Test
    void 오탐과_미탐을_합쳐_뒤집힌_비율을_계산한다() {
        final NicknameAuditQuality quality = new NicknameAuditQuality(100, 10, 5);

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
        assertThat(new NicknameAuditQuality(10, 6, 4).overrideRate()).isEqualTo(1.0);
    }

    @Test
    void 전부_동의하면_0이다() {
        final NicknameAuditQuality quality = new NicknameAuditQuality(50, 0, 0);

        assertThat(quality.overrideRate()).isZero();
        assertThat(quality.agreed()).isEqualTo(50);
    }
}
