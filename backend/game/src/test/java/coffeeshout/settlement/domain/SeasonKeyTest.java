package coffeeshout.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SeasonKeyTest {

    @Test
    void 이벤트_시각의_KST_년월이_시즌_키가_된다() {
        Instant eventTime = Instant.parse("2026-07-15T03:00:00Z");

        assertThat(SeasonKey.from(eventTime).value()).isEqualTo("2026-07");
    }

    @Test
    void 시즌_경계는_KST_기준으로_판정한다() {
        // UTC 6월 30일 15:30 = KST 7월 1일 00:30 — UTC로 판정하면 6월 시즌으로 오귀속된다
        Instant kstNewMonth = Instant.parse("2026-06-30T15:30:00Z");

        assertThat(SeasonKey.from(kstNewMonth).value()).isEqualTo("2026-07");
    }
}
