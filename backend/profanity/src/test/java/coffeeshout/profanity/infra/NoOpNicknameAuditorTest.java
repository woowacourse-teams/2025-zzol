package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NoOpNicknameAuditorTest {

    private static final List<String> 닉네임들 =
            IntStream.range(0, 200).mapToObj(i -> "측정닉%03d".formatted(i)).toList();

    private NoOpNicknameAuditor auditor;

    @BeforeEach
    void setUp() {
        auditor = 스텁(Duration.ZERO, 0);
    }

    private NoOpNicknameAuditor 스텁(Duration latency, double flaggedRatio) {
        return new NoOpNicknameAuditor(NicknameAuditPropertiesFixture.스텁(latency, flaggedRatio));
    }

    private Set<String> flagged된_닉네임(List<NicknameAuditResult> results) {
        return results.stream()
                .filter(result -> result.status() == NicknameAuditStatus.FLAGGED)
                .map(NicknameAuditResult::nickname)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nested
    class audit_검열 {

        @Test
        void 기본값에서는_모든_닉네임을_CLEAN으로_반환한다() {
            final var results = auditor.audit(List.of("용감한호랑이", "씨발", "닉네임"));

            assertThat(results).allMatch(r -> r.status() == NicknameAuditStatus.CLEAN);
        }

        @Test
        void 입력과_동일한_수의_결과를_반환한다() {
            final var results = auditor.audit(List.of("닉네임1", "닉네임2", "닉네임3"));

            assertThat(results).hasSize(3);
        }

        @Test
        void null_입력은_예외가_발생한다() {
            assertThatThrownBy(() -> auditor.audit(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void 빈_리스트는_빈_결과를_반환한다() {
            assertThat(auditor.audit(List.of())).isEmpty();
        }
    }

    @Nested
    class flagged_비율_판정 {

        @Test
        void 비율이_0이면_전부_CLEAN이다() {
            final var results = 스텁(Duration.ZERO, 0).audit(닉네임들);

            assertThat(results).allMatch(r -> r.status() == NicknameAuditStatus.CLEAN);
        }

        @Test
        void 비율이_1이면_전부_FLAGGED이고_차단_조각을_담는다() {
            final var results = 스텁(Duration.ZERO, 1).audit(닉네임들);

            final SoftAssertions softly = new SoftAssertions();
            softly.assertThat(results).allMatch(r -> r.status() == NicknameAuditStatus.FLAGGED);
            softly.assertThat(results)
                    .as("조각이 비면 자동 차단이 닉네임 전체를 사전에 넣어 측정 경로가 실제와 달라진다.")
                    .allMatch(r -> r.profanityTerms().size() == 1);
            softly.assertThat(results)
                    .as("조각은 닉네임의 부분 문자열이어야 도메인이 차단 대상으로 채택한다.")
                    .allMatch(r -> r.nickname().contains(r.profanityTerms().getFirst()));
            softly.assertAll();
        }

        @Test
        void 같은_입력이면_FLAGGED_집합이_회차마다_같다() {
            final Set<String> 첫_회차 = flagged된_닉네임(스텁(Duration.ZERO, 0.5).audit(닉네임들));
            final Set<String> 두번째_회차 = flagged된_닉네임(스텁(Duration.ZERO, 0.5).audit(닉네임들));

            final SoftAssertions softly = new SoftAssertions();
            softly.assertThat(첫_회차).as("판정이 난수면 회차마다 결과가 달라져 측정값을 비교할 수 없다.").isEqualTo(두번째_회차);
            softly.assertThat(첫_회차)
                    .as("비율 0.5가 전부 CLEAN이나 전부 FLAGGED로 쏠리면 위 비교가 아무것도 재지 못한다.")
                    .hasSizeBetween(1, 닉네임들.size() - 1);
            softly.assertAll();
        }
    }

    @Nested
    class latency_지연 {

        private static final Duration 지연 = Duration.ofMillis(50);

        @Test
        void 호출마다_설정한_지연만큼_기다린다() {
            final long 시작 = System.nanoTime();

            스텁(지연, 0).audit(닉네임들);

            assertThat(Duration.ofNanos(System.nanoTime() - 시작)).isGreaterThanOrEqualTo(지연);
        }
    }
}
