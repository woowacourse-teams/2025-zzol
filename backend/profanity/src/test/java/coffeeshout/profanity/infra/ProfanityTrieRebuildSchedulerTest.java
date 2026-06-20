package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.profanity.application.ProfanityFilterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfanityTrieRebuildSchedulerTest {

    @Mock
    private ProfanityFilterService filterService;

    @InjectMocks
    private ProfanityTrieRebuildScheduler scheduler;

    @Nested
    @DisplayName("주기적 재빌드 실행 시")
    class RebuildPeriodically {

        @Test
        @DisplayName("필터 서비스의 트라이 재빌드를 호출한다")
        void 트라이_재빌드를_호출한다() {
            scheduler.rebuildPeriodically();

            verify(filterService, times(1)).rebuildTrie();
        }

        @Test
        @DisplayName("재빌드가 예외를 던져도 전파하지 않고 삼킨다")
        void 재빌드_예외를_삼킨다() {
            doThrow(new RuntimeException("DB 조회 실패")).when(filterService).rebuildTrie();

            assertThatCode(() -> scheduler.rebuildPeriodically())
                    .doesNotThrowAnyException();
            verify(filterService, times(1)).rebuildTrie();
        }
    }
}
