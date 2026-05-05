package coffeeshout.global.zzolbot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AskContextTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

    @Nested
    class stamp_팩터리 {

        @Test
        void 동일한_입력이면_동일한_seed와_asOf를_생성한다() {
            final AskContext ctx1 = AskContext.stamp("A4BX 방 상태 알려줘", List.of(1L, 2L), FIXED_CLOCK);
            final AskContext ctx2 = AskContext.stamp("A4BX 방 상태 알려줘", List.of(1L, 2L), FIXED_CLOCK);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(ctx1.seed()).isEqualTo(ctx2.seed());
                softly.assertThat(ctx1.asOf()).isEqualTo(ctx2.asOf());
            });
        }

        @Test
        void goodIds가_다르면_seed가_달라진다() {
            final AskContext ctx1 = AskContext.stamp("질문", List.of(1L), FIXED_CLOCK);
            final AskContext ctx2 = AskContext.stamp("질문", List.of(2L), FIXED_CLOCK);

            assertThat(ctx1.seed()).isNotEqualTo(ctx2.seed());
        }

        @Test
        void 질문이_다르면_seed가_달라진다() {
            final AskContext ctx1 = AskContext.stamp("질문A", List.of(), FIXED_CLOCK);
            final AskContext ctx2 = AskContext.stamp("질문B", List.of(), FIXED_CLOCK);

            assertThat(ctx1.seed()).isNotEqualTo(ctx2.seed());
        }

        @Test
        void stamp_호출_시_필수_필드가_모두_설정된다() {
            final AskContext ctx = AskContext.stamp("질문", List.of(), FIXED_CLOCK);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(ctx.asOf()).isNotNull();
                softly.assertThat(ctx.requestId()).isNotNull().isNotBlank();
                softly.assertThat(ctx.piiSession()).isNotNull();
            });
        }

        @Test
        void asOf는_Clock에서_주어진_시각으로_설정된다() {
            final Instant fixed = Instant.parse("2025-05-06T12:00:00Z");
            final Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);

            final AskContext ctx = AskContext.stamp("질문", List.of(), clock);

            assertThat(ctx.asOf()).isEqualTo(fixed);
        }

        @Test
        void goodIds_순서가_달라도_seed가_동일하다() {
            final AskContext ctx1 = AskContext.stamp("질문", List.of(1L, 2L, 3L), FIXED_CLOCK);
            final AskContext ctx2 = AskContext.stamp("질문", List.of(3L, 1L, 2L), FIXED_CLOCK);

            assertThat(ctx1.seed()).isEqualTo(ctx2.seed());
        }
    }
}
