package coffeeshout.profanity.domain;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.ProfanityWordFixture;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProfanityWordTest {

    @Nested
    class 유효한_단어_생성 {

        @Test
        void 정상적으로_생성된다() {
            final ProfanityWord word = ProfanityWordFixture.한국어_수동_욕설();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(word.word()).isEqualTo("욕설");
                softly.assertThat(word.language()).isEqualTo(Language.KOREAN);
                softly.assertThat(word.source()).isEqualTo(WordSource.MANUAL);
            });
        }

        @Test
        void 생성자는_입력값을_정규화하지_않는다() {
            final ProfanityWord word = new ProfanityWord("BADWORD", Language.ENGLISH, WordSource.MANUAL, true);

            assertThat(word.word()).isEqualTo("BADWORD");
        }

        @Test
        void 최대_길이_단어는_정상_생성된다() {
            final String maxLengthWord = "가".repeat(ProfanityWord.MAX_WORD_LENGTH);

            final ProfanityWord word = new ProfanityWord(maxLengthWord, Language.KOREAN, WordSource.MANUAL, true);

            assertThat(word.word()).hasSize(ProfanityWord.MAX_WORD_LENGTH);
        }
    }

    @Nested
    class 단어_검증_실패 {

        @Nested
        class null_또는_공백 {

            @Test
            void null이면_예외가_발생한다() {
                assertCoffeeShoutException(
                        () -> ProfanityWord.of(null, Language.KOREAN, WordSource.MANUAL),
                        ProfanityErrorCode.WORD_BLANK
                );
            }

            @Test
            void 빈_문자열이면_예외가_발생한다() {
                assertCoffeeShoutException(
                        () -> ProfanityWord.of("", Language.KOREAN, WordSource.MANUAL),
                        ProfanityErrorCode.WORD_BLANK
                );
            }

            @Test
            void 공백만_있으면_예외가_발생한다() {
                assertCoffeeShoutException(
                        () -> ProfanityWord.of("   ", Language.KOREAN, WordSource.MANUAL),
                        ProfanityErrorCode.WORD_BLANK
                );
            }
        }

        @Nested
        class 최대_길이_초과 {

            @Test
            void 최대_길이를_초과하면_예외가_발생한다() {
                final String tooLong = ProfanityWordFixture.최대_길이_초과_단어();

                assertCoffeeShoutException(
                        () -> ProfanityWord.of(tooLong, Language.KOREAN, WordSource.MANUAL),
                        ProfanityErrorCode.WORD_TOO_LONG
                );
            }
        }

        @Nested
        class null_언어_또는_출처 {

            @Test
            void language가_null이면_예외가_발생한다() {
                assertCoffeeShoutException(
                        () -> ProfanityWord.of("욕설", null, WordSource.MANUAL),
                        ProfanityErrorCode.LANGUAGE_REQUIRED
                );
            }

            @Test
            void source가_null이면_예외가_발생한다() {
                assertCoffeeShoutException(
                        () -> ProfanityWord.of("욕설", Language.KOREAN, null),
                        ProfanityErrorCode.SOURCE_REQUIRED
                );
            }
        }
    }

    @Nested
    class 모든_출처_조합 {

        @Test
        void VANE_출처로_생성된다() {
            final ProfanityWord word = ProfanityWordFixture.한국어_VANE_욕설();

            assertThat(word.source()).isEqualTo(WordSource.VANE);
        }

        @Test
        void LDNOOBW_출처로_생성된다() {
            final ProfanityWord word = ProfanityWordFixture.영어_LDNOOBW_욕설();

            assertThat(word.source()).isEqualTo(WordSource.LDNOOBW);
        }

        @Test
        void AI_FLAGGED_출처로_생성된다() {
            final ProfanityWord word = new ProfanityWord("욕설", Language.KOREAN, WordSource.AI_FLAGGED, true);

            assertThat(word.source()).isEqualTo(WordSource.AI_FLAGGED);
        }
    }

    @Nested
    class 동등성 {

        @Test
        void 같은_단어_언어_출처면_동일하다() {
            final ProfanityWord a = new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true);
            final ProfanityWord b = new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true);

            assertThat(a).isEqualTo(b);
        }

    }
}
