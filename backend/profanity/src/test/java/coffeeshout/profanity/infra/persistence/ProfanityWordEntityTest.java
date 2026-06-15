package coffeeshout.profanity.infra.persistence;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProfanityWordEntityTest {

    @Nested
    class from_도메인_변환 {

        @Test
        void 도메인의_isActive를_그대로_반영한다() {
            final ProfanityWord active = new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true);
            final ProfanityWord inactive = new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, false);

            assertSoftly(softly -> {
                softly.assertThat(ProfanityWordEntity.from(active).isActive()).isTrue();
                softly.assertThat(ProfanityWordEntity.from(inactive).isActive()).isFalse();
            });
        }

        @Test
        void 단어_언어_출처를_도메인과_동일하게_설정한다() {
            final ProfanityWord domain = new ProfanityWord("badword", Language.ENGLISH, WordSource.LDNOOBW, true);

            final ProfanityWordEntity entity = ProfanityWordEntity.from(domain);

            assertSoftly(softly -> {
                softly.assertThat(entity.getWord()).isEqualTo("badword");
                softly.assertThat(entity.getLanguage()).isEqualTo(Language.ENGLISH);
                softly.assertThat(entity.getSource()).isEqualTo(WordSource.LDNOOBW);
            });
        }
    }
}
