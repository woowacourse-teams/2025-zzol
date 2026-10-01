package coffeeshout.profanity.infra.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.support.ServiceTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    @Autowired
    private EntityManager em;

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

        @Test
        void 이미_DEAD_LETTER인_행은_시도_횟수가_더_오르지_않는다() {
            final NicknameAudit audit = auditRepository.save(new NicknameAudit("독닉네임"));
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                recordFailure(audit.getId());
            }

            final int updated = auditRepository.incrementAttemptCount(List.of(audit.getId()));

            assertThat(updated).as("UNAUDITED인 행만 올린다.").isZero();
        }

        /** 실패한 배치 하나를 기록하는 경로. {@code ProfanityAuditBatchProcessor}가 두 쿼리를 한 트랜잭션에서 부른다. */
        private void recordFailure(Long id) {
            auditRepository.incrementAttemptCount(List.of(id));
            auditRepository.markDeadLetterAtAttemptLimit(List.of(id), MAX_ATTEMPTS);
        }
    }

    /**
     * DEAD_LETTER는 검열을 못 끝낸 행이지 판정을 가진 행이 아니다. terminal로 세면 같은 닉네임의
     * UNAUDITED가 "이미 검열됨"으로 오인돼 판정 대신 삭제된다.
     */
    @Nested
    class findNicknamesWithTerminalStatus_판정_보유_판별 {

        @Test
        void DEAD_LETTER만_있는_닉네임은_판정을_가진_것으로_보지_않는다() {
            final NicknameAudit audit = auditRepository.save(new NicknameAudit("독닉네임"));
            audit.updateStatus(NicknameAuditStatus.DEAD_LETTER);
            auditRepository.save(audit);

            assertThat(auditRepository.findNicknamesWithTerminalStatus(List.of("독닉네임")))
                    .isEmpty();
        }

        @Test
        void 검열이_끝난_닉네임은_판정을_가진_것으로_본다() {
            final NicknameAudit audit = auditRepository.save(new NicknameAudit("검열된닉"));
            audit.complete(NicknameAuditStatus.CLEAN, AiConfidence.UNKNOWN, "기존 검열 완료");
            auditRepository.save(audit);

            assertThat(auditRepository.findNicknamesWithTerminalStatus(List.of("검열된닉")))
                    .containsOnly("검열된닉");
        }
    }

    /**
     * 승격 저장이 JDBC 배치 UPDATE({@code bulkUpdateAuditResults})로 바뀌면서 JPA가 대신해주던
     * 타입 변환을 직접 하게 됐다. 값이 실제 DB를 오가며 제대로 들어가는지 다시 읽어 확인한다.
     */
    @Nested
    class bulkUpdateAuditResults_매핑 {

        @Test
        void 승격_대상_두_행만_반영되고_넘기지_않은_행은_그대로_남는다() {
            final NicknameAudit first = auditRepository.save(new NicknameAudit("새닉네임1"));
            final NicknameAudit second = auditRepository.save(new NicknameAudit("새닉네임2"));
            final NicknameAudit untouched = auditRepository.save(new NicknameAudit("건드리면안됨"));

            // MySQL datetime(6) 컬럼은 마이크로초 단위다. Instant.now()는 나노초 정밀도라 절삭하지
            // 않으면 세 시각이 같은 마이크로초 버킷에 떨어져 저장값이 버킷 경계로 반올림되며 구간
            // 밖으로 나갈 수 있다(리눅스 JVM에서 재현, macOS는 클럭이 마이크로초라 항상 통과했다).
            final Instant beforeAudit = Instant.now().truncatedTo(ChronoUnit.MICROS);
            first.complete(NicknameAuditStatus.CLEAN, AiConfidence.of(0.42), "검열 사유1");
            second.complete(NicknameAuditStatus.FLAGGED, AiConfidence.of(0.99), "검열 사유2");
            // 서버가 나노초를 올림할 수 있으니 상한을 한 칸 연다.
            final Instant afterAudit =
                    Instant.now().truncatedTo(ChronoUnit.MICROS).plusNanos(1_000);

            // 두 번째 행을 같이 넘겨야 batchUpdate가 실제로 배치를 태운다. 넘기지 않은 untouched까지
            // 세 행이 테이블에 있어야 WHERE id = ? 가 빠지거나 파라미터 인덱스가 밀려도 잡힌다.
            auditRepository.bulkUpdateAuditResults(List.of(first, second));
            // JDBC UPDATE는 영속성 컨텍스트를 거치지 않아 1차 캐시가 갱신 전 값을 들고 있다.
            // clear() 없이 findById를 부르면 방금 넘긴 인스턴스를 그대로 돌려받아 DB에 실제로
            // 반영됐는지 확인하지 못한다.
            em.clear();

            final NicknameAudit promotedFirst =
                    auditRepository.findById(first.getId()).orElseThrow();
            final NicknameAudit promotedSecond =
                    auditRepository.findById(second.getId()).orElseThrow();
            final NicknameAudit reloadedUntouched =
                    auditRepository.findById(untouched.getId()).orElseThrow();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(promotedFirst.getStatus()).isEqualTo(NicknameAuditStatus.CLEAN);
                softly.assertThat(promotedFirst.getConfidence()).isEqualTo(AiConfidence.of(0.42));
                softly.assertThat(promotedFirst.getReason()).isEqualTo("검열 사유1");
                softly.assertThat(promotedFirst.getAuditedAt()).isBetween(beforeAudit, afterAudit);

                softly.assertThat(promotedSecond.getStatus()).isEqualTo(NicknameAuditStatus.FLAGGED);
                softly.assertThat(promotedSecond.getConfidence()).isEqualTo(AiConfidence.of(0.99));
                softly.assertThat(promotedSecond.getReason()).isEqualTo("검열 사유2");
                softly.assertThat(promotedSecond.getAuditedAt()).isBetween(beforeAudit, afterAudit);

                softly.assertThat(reloadedUntouched.getStatus()).isEqualTo(NicknameAuditStatus.UNAUDITED);
                softly.assertThat(reloadedUntouched.getAuditedAt()).isNull();
            });
        }

        @Test
        void attempt_count는_승격_대상에서_빠져_준영속_엔티티의_낡은_값으로_덮이지_않는다() {
            final NicknameAudit audit = auditRepository.save(new NicknameAudit("새닉네임"));

            // 증가 뒤에 다시 읽으면 인스턴스가 이미 1을 들고 있어 SQL이 attempt_count를 써도 같은 값이
            // 들어간다. 낡은 값이 DB를 덮는 상황을 재현하려면 증가 전에 읽은 이 인스턴스를 그대로 쓴다.
            auditRepository.incrementAttemptCount(List.of(audit.getId()));
            audit.complete(NicknameAuditStatus.CLEAN, AiConfidence.of(0.1), "검열 사유");

            auditRepository.bulkUpdateAuditResults(List.of(audit));
            em.clear();

            assertThat(auditRepository.findById(audit.getId()).orElseThrow().getAttemptCount())
                    .as("bulkUpdateAuditResults는 attempt_count를 갱신 대상에서 뺀다. 누가 SQL에 끼워 넣어도" + " 여기서 잡힌다.")
                    .isEqualTo(1);
        }
    }
}
