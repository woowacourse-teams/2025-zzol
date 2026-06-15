package coffeeshout.profanity.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AiConfidenceTest {

    @Nested
    class 생성_검증 {

        @Test
        void 유효한_범위_내_값으로_생성된다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThatCode(() -> new AiConfidence(BigDecimal.ZERO)).doesNotThrowAnyException();
                softly.assertThatCode(() -> new AiConfidence(BigDecimal.ONE)).doesNotThrowAnyException();
                softly.assertThatCode(() -> new AiConfidence(new BigDecimal("0.5"))).doesNotThrowAnyException();
            });
        }

        @ParameterizedTest
        @ValueSource(strings = {"-0.01", "1.01", "1.5", "-1.0"})
        void 범위를_벗어난_값은_예외가_발생한다(String value) {
            assertThatThrownBy(() -> new AiConfidence(new BigDecimal(value)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void null_값은_예외가_발생한다() {
            assertThatThrownBy(() -> new AiConfidence(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class 스케일_정규화 {

        @Test
        void 소수점_셋째_자리_이상은_반올림된다() {
            final AiConfidence confidence = new AiConfidence(new BigDecimal("0.975"));
            assertThat(confidence.value()).isEqualByComparingTo(new BigDecimal("0.98"));
        }

        @Test
        void 정확히_두_자리인_값은_변경되지_않는다() {
            final AiConfidence confidence = new AiConfidence(new BigDecimal("0.92"));
            assertThat(confidence.value()).isEqualByComparingTo(new BigDecimal("0.92"));
        }
    }

    @Nested
    class 팩토리_메서드 {

        @Test
        void double_값으로_생성된다() {
            final AiConfidence confidence = AiConfidence.of(0.87);
            assertThat(confidence.value()).isEqualByComparingTo(new BigDecimal("0.87"));
        }
    }

    @Nested
    class UNKNOWN_상수 {

        @Test
        void UNKNOWN은_0이다() {
            assertThat(AiConfidence.UNKNOWN.value()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
