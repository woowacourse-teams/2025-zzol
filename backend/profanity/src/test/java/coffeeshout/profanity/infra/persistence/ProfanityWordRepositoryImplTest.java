package coffeeshout.profanity.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.support.ServiceTest;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProfanityWordRepositoryImplTest extends ServiceTest {

    @Autowired
    private ProfanityWordRepositoryImpl repository;

    @Autowired
    private ProfanityWordJpaRepository jpaRepository;

    @Nested
    class save_저장 {

        @Test
        void 신규_단어를_저장하고_true를_반환한다() {
            boolean result = repository.save(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true));

            assertThat(result).isTrue();
            assertThat(jpaRepository.existsByWord("욕설")).isTrue();
        }

        @Test
        void 비활성화된_단어를_재활성화하고_true를_반환한다() {
            jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, false)));

            boolean result = repository.save(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true));

            assertThat(result).isTrue();
            assertThat(jpaRepository.findByWord("욕설").orElseThrow().isActive()).isTrue();
        }

        @Test
        void 이미_활성화된_단어는_false를_반환한다() {
            jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true)));

            boolean result = repository.save(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true));

            assertThat(result).isFalse();
        }

        @Test
        void OPERATOR_ALLOWED_단어에_AI_FLAGGED_저장_시_무시하고_false를_반환한다() {
            jpaRepository.save(ProfanityWordEntity.fromOperatorAllowed("허용닉", Language.KOREAN));

            boolean result = repository.save(new ProfanityWord("허용닉", Language.KOREAN, WordSource.AI_FLAGGED, true));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isFalse();
                softly.assertThat(jpaRepository.findByWord("허용닉").orElseThrow().getSource())
                        .isEqualTo(WordSource.OPERATOR_ALLOWED);
            });
        }

        @Test
        void OPERATOR_ALLOWED_단어에_MANUAL_저장_시_소스를_덮어쓰고_true를_반환한다() {
            jpaRepository.save(ProfanityWordEntity.fromOperatorAllowed("허용닉", Language.KOREAN));

            boolean result = repository.save(new ProfanityWord("허용닉", Language.KOREAN, WordSource.MANUAL, true));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isTrue();
                softly.assertThat(jpaRepository.findByWord("허용닉").orElseThrow().getSource())
                        .isEqualTo(WordSource.MANUAL);
            });
        }
    }

    @Nested
    class bulkInsertIgnore_일괄_저장 {

        @Test
        void 신규_단어를_일괄_삽입하고_삽입_건수를_반환한다() {
            List<ProfanityWord> words = List.of(
                    new ProfanityWord("욕설1", Language.KOREAN, WordSource.MANUAL, true),
                    new ProfanityWord("욕설2", Language.KOREAN, WordSource.MANUAL, true),
                    new ProfanityWord("badword", Language.ENGLISH, WordSource.LDNOOBW, true)
            );

            int count = repository.bulkInsertIgnore(words);

            assertThat(count).isEqualTo(3);
        }

        @Test
        void 중복_단어는_무시하고_신규_단어만_삽입된다() {
            // JDBC 삽입 전 JPA 캐시를 DB에 반영해야 INSERT IGNORE가 중복을 감지한다
            jpaRepository.saveAndFlush(ProfanityWordEntity.from(
                    new ProfanityWord("기존단어", Language.KOREAN, WordSource.MANUAL, true)));

            int count = repository.bulkInsertIgnore(List.of(
                    new ProfanityWord("기존단어", Language.KOREAN, WordSource.MANUAL, true),
                    new ProfanityWord("신규단어", Language.KOREAN, WordSource.MANUAL, true)
            ));

            assertThat(count).isEqualTo(1);
        }

        @Test
        void 빈_목록은_0을_반환한다() {
            int count = repository.bulkInsertIgnore(List.of());

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    class operatorAllow_운영자_허용 {

        @Test
        void 기존_단어의_출처를_OPERATOR_ALLOWED로_변경한다() {
            jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true)));

            repository.operatorAllow("욕설", Language.KOREAN);

            assertThat(jpaRepository.findByWord("욕설").orElseThrow().getSource())
                    .isEqualTo(WordSource.OPERATOR_ALLOWED);
        }

        @Test
        void 없는_단어는_OPERATOR_ALLOWED로_신규_저장한다() {
            repository.operatorAllow("허용닉네임", Language.KOREAN);

            ProfanityWordEntity entity = jpaRepository.findByWord("허용닉네임").orElseThrow();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(entity.getSource()).isEqualTo(WordSource.OPERATOR_ALLOWED);
                softly.assertThat(entity.isActive()).isTrue();
            });
        }
    }

    @Nested
    class deactivate_비활성화 {

        @Test
        void 단어를_비활성화한다() {
            jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true)));

            repository.deactivate("욕설");

            assertThat(jpaRepository.findByWord("욕설").orElseThrow().isActive()).isFalse();
        }
    }

    @Nested
    class activate_활성화 {

        @Test
        void 단어를_활성화한다() {
            jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, false)));

            repository.activate("욕설");

            assertThat(jpaRepository.findByWord("욕설").orElseThrow().isActive()).isTrue();
        }
    }
}
