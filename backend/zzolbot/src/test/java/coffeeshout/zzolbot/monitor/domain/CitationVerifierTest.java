package coffeeshout.zzolbot.monitor.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CitationVerifierTest {

    private static final List<String> LOGS = List.of(
            "[2026-08-26 14:00:05.112] [ERROR] --- [pool-1-thread-2] c.g.h.RedisStreamContainerRecovery : 복구 실패",
            "[2026-08-26 14:01:10.456] [ERROR] --- [http-nio-8080-exec-5] c.web.RestExceptionHandler : 5xx");

    @Nested
    class 인용이_실재하면_통과한다 {

        @Test
        void 로그_줄_전체를_그대로_인용하면_통과한다() {
            assertThat(CitationVerifier.citedInLogs(LOGS.get(0), LOGS)).isTrue();
        }

        @Test
        void 로그_줄의_일부만_인용해도_통과한다() {
            assertThat(CitationVerifier.citedInLogs("c.g.h.RedisStreamContainerRecovery : 복구 실패", LOGS))
                    .isTrue();
        }

        @Test
        void 줄바꿈과_연속_공백은_무시한다() {
            // 스택 트레이스처럼 로그 한 건이 여러 줄일 수 있어 공백을 접은 뒤 비교한다
            assertThat(CitationVerifier.citedInLogs("[ERROR]   ---\n [pool-1-thread-2]", LOGS))
                    .isTrue();
        }
    }

    @Nested
    class 인용이_없거나_지어냈으면_막는다 {

        @Test
        void 로그에_없는_줄을_인용하면_막는다() {
            assertThat(CitationVerifier.citedInLogs("[2026-08-26 14:00:05.999] [ERROR] 지어낸 로그", LOGS))
                    .isFalse();
        }

        @Test
        void 밀리초_두_자리가_달라도_막는다() {
            // 실제로 관찰된 전사 오류다. 옳은 줄을 고르고 글자를 틀린다
            assertThat(CitationVerifier.citedInLogs(LOGS.get(0).replace("14:00:05.112", "14:00:05.122"), LOGS))
                    .isFalse();
        }

        @Test
        void 빈_인용은_막는다() {
            assertThat(CitationVerifier.citedInLogs("   ", LOGS)).isFalse();
        }

        @Test
        void 인용이_null이면_막는다() {
            assertThat(CitationVerifier.citedInLogs(null, LOGS)).isFalse();
        }

        @Test
        void 로그_샘플이_null이면_막는다() {
            assertThat(CitationVerifier.citedInLogs("아무거나", null)).isFalse();
        }

        @Test
        void 로그_샘플에_null_원소가_있어도_터지지_않는다() {
            assertThat(CitationVerifier.citedInLogs("5xx", Arrays.asList(null, LOGS.get(1))))
                    .isTrue();
        }
    }
}
