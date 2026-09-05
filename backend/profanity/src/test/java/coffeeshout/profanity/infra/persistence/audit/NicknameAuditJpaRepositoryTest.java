package coffeeshout.profanity.infra.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.support.ServiceTest;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NicknameAuditJpaRepositoryTest extends ServiceTest {

    /** {@code service.yml}의 nickname-audit.max-attempts 기본값. */
    private static final int MAX_ATTEMPTS = 3;

    @Autowired
    private NicknameAuditJpaRepository auditRepository;

    /**
     * 검열 호출이 되풀이 실패하는 행을 큐에서 빼는 경로다(#1759).
     *
     * <p>세지 않으면 파싱 실패를 일으키는 닉네임 하나가 회차마다 같은 실패를 낸다. 0단계(#1752)가 막은 건
     * 회차 안의 반복이고, 회차 사이의 반복은 이 시도 횟수가 막는다.
     */
    @Nested
    class recordFailure_시도_횟수 {

        @Test
        void 상한에_닿기_전에는_UNAUDITED로_남아_다음_회차에_다시_시도된다() {
            final NicknameAudit audit = auditRepository.save(new NicknameAudit("독닉네임"));

            recordFailure(audit.getId());
            recordFailure(audit.getId());

            final NicknameAudit reloaded =
                    auditRepository.findById(audit.getId()).orElseThrow();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(reloaded.getAttemptCount()).isEqualTo(2);
                softly.assertThat(reloaded.getStatus()).isEqualTo(NicknameAuditStatus.UNAUDITED);
                softly.assertThat(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED))
                        .isEqualTo(1);
            });
        }

        @Test
        void 상한에_닿으면_DEAD_LETTER가_되어_UNAUDITED_스캔에서_빠진다() {
            final NicknameAudit audit = auditRepository.save(new NicknameAudit("독닉네임"));

            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                recordFailure(audit.getId());
            }

            final NicknameAudit reloaded =
                    auditRepository.findById(audit.getId()).orElseThrow();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(reloaded.getStatus()).isEqualTo(NicknameAuditStatus.DEAD_LETTER);
                softly.assertThat(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED))
                        .as("DEAD_LETTER가 스캔에 남으면 그 행이 회차마다 같은 실패를 되풀이한다.")
                        .isZero();
            });
        }

        @Test
        void 실패한_배치에_없는_행은_시도_횟수가_오르지_않는다() {
            final NicknameAudit failed = auditRepository.save(new NicknameAudit("실패닉네임"));
            final NicknameAudit untouched = auditRepository.save(new NicknameAudit("멀쩡닉네임"));

            recordFailure(failed.getId());

            assertThat(auditRepository.findById(untouched.getId()).orElseThrow().getAttemptCount())
                    .isZero();
        }

        /** 실패한 배치 하나를 기록하는 경로. {@code ProfanityAuditBatchProcessor}가 두 쿼리를 한 트랜잭션에서 부른다. */
        private void recordFailure(Long id) {
            auditRepository.incrementAttemptCount(List.of(id));
            auditRepository.markDeadLetterAtAttemptLimit(List.of(id), MAX_ATTEMPTS);
        }
    }
}
