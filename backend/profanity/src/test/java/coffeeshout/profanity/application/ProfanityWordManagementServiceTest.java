package coffeeshout.profanity.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.fixture.ProfanityWordFixture;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityErrorCode;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.TrieRefreshNotifier;
import coffeeshout.profanity.domain.WordSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProfanityWordManagementServiceTest {

    private ProfanityWordRepository wordRepository;
    private TrieRefreshNotifier trieRefreshPort;
    private ProfanityWordManagementService service;

    @BeforeEach
    void setUp() {
        wordRepository = mock(ProfanityWordRepository.class);
        trieRefreshPort = mock(TrieRefreshNotifier.class);
        service = new ProfanityWordManagementService(wordRepository, trieRefreshPort);
    }

    @Nested
    class add_단어_등록 {

        @Test
        void 유효한_단어를_저장하고_트라이_갱신을_발행한다() {
            given(wordRepository.save(any(ProfanityWord.class))).willReturn(true);

            service.add("욕설", Language.KOREAN, WordSource.MANUAL);

            then(wordRepository).should().save(any(ProfanityWord.class));
            then(trieRefreshPort).should().publish();
        }

        @Test
        void 이미_활성화된_단어는_트라이_갱신을_발행하지_않는다() {
            given(wordRepository.save(any(ProfanityWord.class))).willReturn(false);

            service.add("욕설", Language.KOREAN, WordSource.MANUAL);

            then(trieRefreshPort).should(never()).publish();
        }

        @Test
        void 앞뒤_공백이_제거된_단어가_저장된다() {
            service.add("  욕설  ", Language.KOREAN, WordSource.MANUAL);

            then(wordRepository).should().save(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL));
        }

        @Test
        void 대문자_단어는_소문자로_정규화되어_저장된다() {
            service.add("BADWORD", Language.ENGLISH, WordSource.LDNOOBW);

            then(wordRepository).should().save(new ProfanityWord("badword", Language.ENGLISH, WordSource.LDNOOBW));
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
            then(trieRefreshPort).should().publish();
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

            then(trieRefreshPort).should(never()).publish();
        }
    }

    @Nested
    class saveAll_일괄_등록 {

        @Test
        void 여러_단어를_일괄_저장하고_트라이_갱신을_한_번_발행한다() {
            final List<ProfanityWord> words = List.of(
                    ProfanityWordFixture.한국어_수동_욕설(),
                    ProfanityWordFixture.영어_LDNOOBW_욕설()
            );

            service.saveAll(words);

            then(wordRepository).should().save(words.get(0));
            then(wordRepository).should().save(words.get(1));
            then(trieRefreshPort).should().publish();
        }
    }

    @Nested
    class findAll_조회 {

        @Test
        void 전체_단어_목록을_반환한다() {
            final List<ProfanityWord> expected = List.of(ProfanityWordFixture.한국어_수동_욕설());
            given(wordRepository.findAll()).willReturn(expected);

            assertThat(service.findAll()).isEqualTo(expected);
        }

        @Test
        void 활성_단어_목록을_반환한다() {
            final List<ProfanityWord> expected = List.of(ProfanityWordFixture.한국어_수동_욕설());
            given(wordRepository.findAllActive()).willReturn(expected);

            assertThat(service.findAllActive()).isEqualTo(expected);
        }
    }
}
