package coffeeshout.admin.quality.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.admin.quality.domain.NicknameAuditQuality;
import coffeeshout.admin.quality.domain.QualityStatisticsRepository;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameFeedback;
import coffeeshout.profanity.domain.audit.NicknameFeedback.OperatorDecision;
import coffeeshout.profanity.infra.persistence.audit.NicknameFeedbackJpaRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AI 판정과 관리자 결정의 네 조합을 GROUP BY 로 한 번에 세는 쿼리라
 * 실제 DB 에서만 조합이 제대로 갈리는지 확인된다.
 */
@DisplayName("QueryDslQualityStatisticsRepository")
class QueryDslQualityStatisticsRepositoryTest extends AdminModuleServiceTest {

    private static final Instant FROM = Instant.parse("2020-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2099-01-01T00:00:00Z");

    @Autowired
    private QualityStatisticsRepository qualityStatisticsRepository;

    @Autowired
    private NicknameFeedbackJpaRepository nicknameFeedbackJpaRepository;

    private void saveFeedback(String nickname, boolean aiFlagged, OperatorDecision decision) {
        nicknameFeedbackJpaRepository.save(
                new NicknameFeedback(nickname, aiFlagged, AiConfidence.of(0.9), decision, "사유"));
    }

    @Nested
    class findNicknameAuditQuality {

        @Test
        void AI가_걸렀는데_관리자가_허용하면_오탐이다() {
            saveFeedback("멀쩡한닉", true, OperatorDecision.ALLOWED);

            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.falsePositive()).isEqualTo(1);
            assertThat(quality.falseNegative()).isZero();
        }

        @Test
        void AI가_놓쳤는데_관리자가_차단하면_미탐이다() {
            saveFeedback("걸러야할닉", false, OperatorDecision.BLOCKED);

            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.falseNegative()).isEqualTo(1);
            assertThat(quality.falsePositive()).isZero();
        }

        @Test
        void AI와_관리자가_동의한_건은_뒤집힘으로_세지_않는다() {
            saveFeedback("욕설닉", true, OperatorDecision.BLOCKED);
            saveFeedback("보통닉", false, OperatorDecision.ALLOWED);

            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.total()).isEqualTo(2);
            assertThat(quality.agreed()).isEqualTo(2);
            assertThat(quality.overrideRate()).isZero();
        }

        @Test
        void 네_조합을_한_번에_집계한다() {
            saveFeedback("a", true, OperatorDecision.BLOCKED);
            saveFeedback("b", true, OperatorDecision.BLOCKED);
            saveFeedback("c", true, OperatorDecision.ALLOWED);
            saveFeedback("d", false, OperatorDecision.BLOCKED);
            saveFeedback("e", false, OperatorDecision.ALLOWED);

            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.total()).isEqualTo(5);
            assertThat(quality.falsePositive()).isEqualTo(1);
            assertThat(quality.falseNegative()).isEqualTo(1);
            assertThat(quality.agreed()).isEqualTo(3);
            assertThat(quality.overrideRate()).isEqualTo(0.4);
        }

        @Test
        void 기간_밖의_판정은_세지_않는다() {
            saveFeedback("a", true, OperatorDecision.ALLOWED);

            final NicknameAuditQuality quality =
                    qualityStatisticsRepository.findNicknameAuditQuality(FROM, Instant.parse("2020-01-02T00:00:00Z"));

            assertThat(quality.total()).isZero();
        }

        @Test
        void 판정이_없으면_전부_0이다() {
            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.total()).isZero();
            assertThat(quality.overrideRate()).isZero();
        }
    }

    @Nested
    class 신고_통계 {

        @Test
        void 미처리_신고가_없으면_대기_건수가_0이고_가장_오래된_건이_없다() {
            assertThat(qualityStatisticsRepository.countPendingReports()).isZero();
            assertThat(qualityStatisticsRepository.findOldestPendingReportCreatedAt())
                    .isEmpty();
        }
    }
}
