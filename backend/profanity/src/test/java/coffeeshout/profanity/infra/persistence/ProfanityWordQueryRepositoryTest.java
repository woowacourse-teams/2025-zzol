package coffeeshout.profanity.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.support.ServiceTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

class ProfanityWordQueryRepositoryTest extends ServiceTest {

    @Autowired
    private ProfanityWordQueryRepository queryRepository;

    @Autowired
    private ProfanityWordJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("씨발", Language.KOREAN, WordSource.MANUAL, true)));
        jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("개새끼", Language.KOREAN, WordSource.VANE, true)));
        jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("fuck", Language.ENGLISH, WordSource.MANUAL, true)));
        jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("badword", Language.ENGLISH, WordSource.LDNOOBW, false)));
        jpaRepository.save(ProfanityWordEntity.from(new ProfanityWord("허용닉네임", Language.KOREAN, WordSource.OPERATOR_ALLOWED, true)));
    }

    @Nested
    class 필터_없이_전체_조회 {

        @Test
        void 모든_단어를_반환한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, null, null, null, PageRequest.of(0, 20));

            assertThat(result.getTotalElements()).isEqualTo(5);
        }
    }

    @Nested
    class search_검색어_필터 {

        @Test
        void 검색어를_포함하는_단어만_반환한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged("씨", null, null, null, PageRequest.of(0, 20));

            assertThat(result.getContent())
                    .extracting(ProfanityWordEntity::getWord)
                    .containsExactly("씨발");
        }

        @Test
        void 검색어는_대소문자를_구분하지_않는다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged("FUCK", null, null, null, PageRequest.of(0, 20));

            assertThat(result.getContent())
                    .extracting(ProfanityWordEntity::getWord)
                    .containsExactly("fuck");
        }

        @Test
        void 일치하는_단어가_없으면_빈_결과를_반환한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged("xyz", null, null, null, PageRequest.of(0, 20));

            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        void 공백_검색어는_전체_조회와_동일하다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged("  ", null, null, null, PageRequest.of(0, 20));

            assertThat(result.getTotalElements()).isEqualTo(5);
        }
    }

    @Nested
    class language_언어_필터 {

        @Test
        void 한국어_단어만_반환한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, Language.KOREAN, null, null, PageRequest.of(0, 20));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getTotalElements()).isEqualTo(3);
                softly.assertThat(result.getContent())
                        .extracting(ProfanityWordEntity::getLanguage)
                        .containsOnly(Language.KOREAN);
            });
        }

        @Test
        void 영어_단어만_반환한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, Language.ENGLISH, null, null, PageRequest.of(0, 20));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getTotalElements()).isEqualTo(2);
                softly.assertThat(result.getContent())
                        .extracting(ProfanityWordEntity::getLanguage)
                        .containsOnly(Language.ENGLISH);
            });
        }
    }

    @Nested
    class source_출처_필터 {

        @Test
        void 특정_출처의_단어만_반환한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, null, WordSource.MANUAL, null, PageRequest.of(0, 20));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getTotalElements()).isEqualTo(2);
                softly.assertThat(result.getContent())
                        .extracting(ProfanityWordEntity::getSource)
                        .containsOnly(WordSource.MANUAL);
            });
        }
    }

    @Nested
    class activeOnly_활성_필터 {

        @Test
        void true면_활성_단어만_반환한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, null, null, true, PageRequest.of(0, 20));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getTotalElements()).isEqualTo(4);
                softly.assertThat(result.getContent())
                        .extracting(ProfanityWordEntity::isActive)
                        .containsOnly(true);
            });
        }

        @Test
        void false면_비활성_단어만_반환한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, null, null, false, PageRequest.of(0, 20));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getTotalElements()).isEqualTo(1);
                softly.assertThat(result.getContent())
                        .extracting(ProfanityWordEntity::isActive)
                        .containsOnly(false);
            });
        }
    }

    @Nested
    class 복합_필터 {

        @Test
        void 언어와_활성_필터를_동시에_적용한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, Language.KOREAN, null, true, PageRequest.of(0, 20));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getTotalElements()).isEqualTo(3);
                softly.assertThat(result.getContent())
                        .extracting(ProfanityWordEntity::getLanguage)
                        .containsOnly(Language.KOREAN);
                softly.assertThat(result.getContent())
                        .extracting(ProfanityWordEntity::isActive)
                        .containsOnly(true);
            });
        }

        @Test
        void 검색어와_언어_필터를_동시에_적용한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged("bad", Language.ENGLISH, null, null, PageRequest.of(0, 20));

            assertThat(result.getContent())
                    .extracting(ProfanityWordEntity::getWord)
                    .containsExactly("badword");
        }
    }

    @Nested
    class 페이징 {

        @Test
        void 페이지_크기만큼만_반환하고_전체_카운트는_유지된다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, null, null, null, PageRequest.of(0, 2));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getContent()).hasSize(2);
                softly.assertThat(result.getTotalElements()).isEqualTo(5);
            });
        }

        @Test
        void 두번째_페이지를_반환한다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, null, null, null, PageRequest.of(1, 2));

            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        void 영어_단어는_알파벳_오름차순으로_정렬된다() {
            Page<ProfanityWordEntity> result = queryRepository.findAllPaged(null, Language.ENGLISH, null, null, PageRequest.of(0, 20));

            assertThat(result.getContent())
                    .extracting(ProfanityWordEntity::getWord)
                    .containsExactly("badword", "fuck");
        }
    }
}
