package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import coffeeshout.fixture.ProfanityWordFixture;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.WordSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProfanityFilterServiceTest {

    private ProfanityWordRepository wordRepository;
    private SimpleMeterRegistry meterRegistry;
    private ProfanityFilterService service;

    @BeforeEach
    void setUp() {
        wordRepository = mock(ProfanityWordRepository.class);
        meterRegistry = new SimpleMeterRegistry();
        service = new ProfanityFilterService(wordRepository, new TextNormalizer(), meterRegistry);

        given(wordRepository.findAllActive())
                .willReturn(List.of(ProfanityWordFixture.한국어_수동_욕설(), ProfanityWordFixture.영어_LDNOOBW_욕설()));
        service.init();
    }

    @Nested
    class contains_비속어_감지 {

        @Test
        void 한국어_비속어가_포함된_텍스트는_true를_반환한다() {
            assertThat(service.contains("이건 욕설이야")).isTrue();
        }

        @Test
        void 영어_비속어가_포함된_텍스트는_true를_반환한다() {
            assertThat(service.contains("this is a badword")).isTrue();
        }

        @Test
        void 비속어가_없는_텍스트는_false를_반환한다() {
            assertThat(service.contains("안녕하세요 좋은 하루 보내세요")).isFalse();
        }

        @Test
        void 대소문자_구분_없이_감지된다() {
            assertThat(service.contains("BADWORD")).isTrue();
        }

        @Test
        void null이면_false를_반환한다() {
            assertThat(service.contains(null)).isFalse();
        }

        @Test
        void 빈_문자열이면_false를_반환한다() {
            assertThat(service.contains("")).isFalse();
        }

        @Test
        void 공백만_있으면_false를_반환한다() {
            assertThat(service.contains("   ")).isFalse();
        }
    }

    @Nested
    class contains_리트스피크_우회_감지 {

        @Test
        void 특수문자_삽입_우회를_감지한다() {
            assertThat(service.contains("b@dword")).isTrue();
        }

        @Test
        void 공백_삽입_우회를_감지한다() {
            assertThat(service.contains("bad word")).isTrue();
        }
    }

    @Nested
    class rebuildTrie_재구성 {

        @Test
        void 트라이_재구성_후_새로운_단어를_감지한다() {
            given(wordRepository.findAllActive())
                    .willReturn(List.of(new ProfanityWord("신규욕설", Language.KOREAN, WordSource.MANUAL, true)));
            service.rebuildTrie();

            assertThat(service.contains("이건 신규욕설이야")).isTrue();
        }

        @Test
        void 트라이_재구성_후_제거된_단어는_감지되지_않는다() {
            given(wordRepository.findAllActive()).willReturn(List.of());
            service.rebuildTrie();

            assertThat(service.contains("욕설")).isFalse();
        }
    }

    /**
     * FLAGGED 한 건마다 pub/sub이 나가고 구독자가 여기서 트라이를 통째로 다시 만든다.
     * 재빌드 시간을 안 재면 이게 병목인지 아닌지를 추측으로 답하게 된다.
     */
    @Nested
    class rebuildTrie_재구성_시간 {

        @Test
        void 재구성마다_소요_시간이_기록된다() {
            service.rebuildTrie();

            assertThat(meterRegistry
                            .get("profanity.trie.rebuild.duration")
                            .timer()
                            .count())
                    .as("init 1회에 방금 호출 1회를 더해 2회다.")
                    .isEqualTo(2L);
        }
    }
}
