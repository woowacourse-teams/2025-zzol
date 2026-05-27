package coffeeshout.profanity.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.fixture.ProfanityWordFixture;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityErrorCode;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.TrieRefreshNotifier;
import coffeeshout.profanity.domain.WordSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class ProfanityWordManagementServiceTest {

    private ProfanityWordRepository wordRepository;
    private TrieRefreshNotifier trieRefreshNotifier;
    private ProfanityWordManagementService service;

    @BeforeEach
    void setUp() {
        wordRepository = mock(ProfanityWordRepository.class);
        trieRefreshNotifier = mock(TrieRefreshNotifier.class);
        service = new ProfanityWordManagementService(wordRepository, trieRefreshNotifier, new TextNormalizer());
    }

    @Nested
    class add_단어_등록 {

        @Test
        void 유효한_단어를_저장하고_트라이_갱신을_발행한다() {
            given(wordRepository.save(any(ProfanityWord.class))).willReturn(true);

            service.add("욕설", Language.KOREAN, WordSource.MANUAL);

            then(wordRepository).should().save(any(ProfanityWord.class));
            then(trieRefreshNotifier).should().publish();
        }

        @Test
        void 이미_활성화된_단어는_트라이_갱신을_발행하지_않는다() {
            given(wordRepository.save(any(ProfanityWord.class))).willReturn(false);

            service.add("욕설", Language.KOREAN, WordSource.MANUAL);

            then(trieRefreshNotifier).should(never()).publish();
        }

        @Test
        void 앞뒤_공백이_제거된_단어가_저장된다() {
            service.add("  욕설  ", Language.KOREAN, WordSource.MANUAL);

            then(wordRepository).should().save(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true));
        }

        @Test
        void 대문자_단어는_소문자로_정규화되어_저장된다() {
            service.add("BADWORD", Language.ENGLISH, WordSource.LDNOOBW);

            then(wordRepository).should().save(new ProfanityWord("badword", Language.ENGLISH, WordSource.LDNOOBW, true));
        }

        @Test
        void 리트_문자가_치환되어_저장된다() {
            service.add("h3ll0", Language.ENGLISH, WordSource.MANUAL);

            then(wordRepository).should().save(new ProfanityWord("hello", Language.ENGLISH, WordSource.MANUAL, true));
        }
    }

    @Nested
    class deactivate_단어_비활성화 {

        @Test
        void 존재하는_단어를_비활성화하고_트라이_갱신을_발행한다() {
            final ProfanityWord word = ProfanityWordFixture.한국어_수동_욕설();
            given(wordRepository.findByWord("욕설")).willReturn(Optional.of(word));

            service.deactivate("욕설");

            then(wordRepository).should().deactivate("욕설");
            then(trieRefreshNotifier).should().publish();
        }

        @Test
        void 대소문자가_정규화된_단어를_비활성화한다() {
            final ProfanityWord word = ProfanityWordFixture.영어_LDNOOBW_욕설();
            given(wordRepository.findByWord("badword")).willReturn(Optional.of(word));

            service.deactivate("BADWORD");

            then(wordRepository).should().deactivate("badword");
        }

        @Test
        void 존재하지_않는_단어는_예외가_발생한다() {
            given(wordRepository.findByWord(any())).willReturn(Optional.empty());

            assertCoffeeShoutException(
                    () -> service.deactivate("없는단어"),
                    ProfanityErrorCode.WORD_NOT_FOUND
            );
        }

        @Test
        void 예외_발생_시_트라이_갱신이_발행되지_않는다() {
            given(wordRepository.findByWord(any())).willReturn(Optional.empty());

            try {
                service.deactivate("없는단어");
            } catch (Exception ignored) {
            }

            then(trieRefreshNotifier).should(never()).publish();
        }
    }

    @Nested
    class findByWord_단어_조회 {

        @Test
        void 존재하는_단어를_반환한다() {
            final ProfanityWord word = ProfanityWordFixture.한국어_수동_욕설();
            given(wordRepository.findByWord("욕설")).willReturn(Optional.of(word));

            assertThat(service.findByWord("욕설")).contains(word);
        }

        @Test
        void 존재하지_않는_단어는_빈_Optional을_반환한다() {
            given(wordRepository.findByWord(any())).willReturn(Optional.empty());

            assertThat(service.findByWord("없는단어")).isEmpty();
        }

        @Test
        void 조회_시_단어가_정규화된다() {
            final ProfanityWord word = ProfanityWordFixture.영어_LDNOOBW_욕설();
            given(wordRepository.findByWord("badword")).willReturn(Optional.of(word));

            assertThat(service.findByWord("BADWORD")).contains(word);
        }
    }

    @Nested
    class operatorAllow_운영자_허용 {

        @Test
        void repository에_위임하고_트라이_갱신을_발행한다() {
            service.operatorAllow("허용닉네임");

            then(wordRepository).should().operatorAllow("허용닉네임", Language.KOREAN);
            then(trieRefreshNotifier).should().publish();
        }

        @Test
        void 정규화된_단어로_repository에_위임한다() {
            service.operatorAllow("  HELLO  ");

            then(wordRepository).should().operatorAllow("hello", Language.ENGLISH);
        }
    }

    @Nested
    class isOperatorAllowed_운영자_허용_여부 {

        @Test
        void OPERATOR_ALLOWED_source_단어는_true를_반환한다() {
            given(wordRepository.findByWord("허용닉네임"))
                    .willReturn(Optional.of(ProfanityWordFixture.운영자_허용_단어()));

            assertThat(service.isOperatorAllowed("허용닉네임")).isTrue();
        }

        @Test
        void MANUAL_source_단어는_false를_반환한다() {
            given(wordRepository.findByWord("욕설"))
                    .willReturn(Optional.of(ProfanityWordFixture.한국어_수동_욕설()));

            assertThat(service.isOperatorAllowed("욕설")).isFalse();
        }

        @Test
        void AI_FLAGGED_source_단어는_false를_반환한다() {
            given(wordRepository.findByWord("욕설닉네임"))
                    .willReturn(Optional.of(ProfanityWordFixture.한국어_AI_FLAGGED_욕설()));

            assertThat(service.isOperatorAllowed("욕설닉네임")).isFalse();
        }

        @Test
        void 존재하지_않는_단어는_false를_반환한다() {
            given(wordRepository.findByWord(any())).willReturn(Optional.empty());

            assertThat(service.isOperatorAllowed("없는단어")).isFalse();
        }
    }

    @Nested
    class activate_단어_활성화 {

        @Test
        void 존재하는_단어를_활성화하고_트라이_갱신을_발행한다() {
            final ProfanityWord word = ProfanityWordFixture.한국어_수동_욕설();
            given(wordRepository.findByWord("욕설")).willReturn(Optional.of(word));

            service.activate("욕설");

            then(wordRepository).should().activate("욕설");
            then(trieRefreshNotifier).should().publish();
        }

        @Test
        void 대소문자가_정규화된_단어를_활성화한다() {
            final ProfanityWord word = ProfanityWordFixture.영어_LDNOOBW_욕설();
            given(wordRepository.findByWord("badword")).willReturn(Optional.of(word));

            service.activate("BADWORD");

            then(wordRepository).should().activate("badword");
        }

        @Test
        void 존재하지_않는_단어는_예외가_발생한다() {
            given(wordRepository.findByWord(any())).willReturn(Optional.empty());

            assertCoffeeShoutException(
                    () -> service.activate("없는단어"),
                    ProfanityErrorCode.WORD_NOT_FOUND
            );
        }

        @Test
        void 예외_발생_시_트라이_갱신이_발행되지_않는다() {
            given(wordRepository.findByWord(any())).willReturn(Optional.empty());

            try {
                service.activate("없는단어");
            } catch (Exception ignored) {
            }

            then(trieRefreshNotifier).should(never()).publish();
        }
    }

    @Nested
    class findAllPaged_필터링_조회 {

        @Test
        void 검색어와_필터를_repository에_위임하고_결과를_반환한다() {
            final List<ProfanityWord> words = List.of(ProfanityWordFixture.한국어_수동_욕설());
            final Page<ProfanityWord> expected = new PageImpl<>(words);
            given(wordRepository.findAllPaged(eq("욕"), eq(Language.KOREAN), eq(WordSource.MANUAL), eq(true), any(Pageable.class)))
                    .willReturn(expected);

            final Page<ProfanityWord> result = service.findAllPaged("욕", Language.KOREAN, WordSource.MANUAL, true, 0, 20);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void null_검색어는_빈_문자열로_변환되어_전달된다() {
            given(wordRepository.findAllPaged(anyString(), any(), any(), any(), any(Pageable.class)))
                    .willReturn(Page.empty());

            service.findAllPaged(null, null, null, null, 0, 20);

            then(wordRepository).should().findAllPaged(eq(""), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        void 앞뒤_공백_검색어는_제거된_후_전달된다() {
            given(wordRepository.findAllPaged(anyString(), any(), any(), any(), any(Pageable.class)))
                    .willReturn(Page.empty());

            service.findAllPaged("  욕  ", null, null, null, 0, 20);

            then(wordRepository).should().findAllPaged(eq("욕"), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        void 언어_출처_활성여부_필터가_null이면_repository에_null로_전달된다() {
            given(wordRepository.findAllPaged(anyString(), any(), any(), any(), any(Pageable.class)))
                    .willReturn(Page.empty());

            service.findAllPaged("", null, null, null, 0, 20);

            then(wordRepository).should().findAllPaged(eq(""), isNull(), isNull(), isNull(), any(Pageable.class));
        }
    }
}
