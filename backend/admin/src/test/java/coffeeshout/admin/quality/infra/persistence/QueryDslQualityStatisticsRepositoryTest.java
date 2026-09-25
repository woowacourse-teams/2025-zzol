package coffeeshout.admin.quality.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.admin.quality.domain.NicknameAuditQuality;
import coffeeshout.admin.quality.domain.QualityStatisticsRepository;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditJpaRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 표본 여부와 운영자 결정의 조합을 GROUP BY 로 한 번에 세는 쿼리라
 * 실제 DB 에서만 조합이 제대로 갈리는지 확인된다.
 */
@DisplayName("QueryDslQualityStatisticsRepository")
class QueryDslQualityStatisticsRepositoryTest extends AdminModuleServiceTest {

    private static final Instant FROM = Instant.parse("2020-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2099-01-01T00:00:00Z");

    @Autowired
    private QualityStatisticsRepository qualityStatisticsRepository;

    @Autowired
    private NicknameAuditJpaRepository nicknameAuditJpaRepository;

    /** 표본은 AI가 CLEAN으로 통과시킨 행, 표본이 아니면 AI가 걸러 운영자에게 넘어온 행이다. */
    private void saveDecided(String nickname, boolean reviewSample, NicknameAuditStatus decision) {
        final NicknameAudit audit = save(nickname, decision);
        if (reviewSample) {
            audit.markReviewSample();
        }
        nicknameAuditJpaRepository.save(audit);
    }

    private NicknameAudit save(String nickname, NicknameAuditStatus status) {
        final NicknameAudit audit = new NicknameAudit(nickname);
        audit.complete(status, AiConfidence.of(0.9), "사유");
        return nicknameAuditJpaRepository.save(audit);
    }

    @Nested
    class findNicknameAuditQuality {

        @Test
        void AI가_걸렀는데_관리자가_허용하면_오탐이다() {
            saveDecided("멀쩡한닉", false, NicknameAuditStatus.ALLOWED);

            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.falsePositive()).isEqualTo(1);
            assertThat(quality.falseNegative()).isZero();
        }

        @Test
        void 표본을_관리자가_차단하면_미탐이다() {
            saveDecided("걸러야할닉", true, NicknameAuditStatus.BLOCKED);

            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.falseNegative()).isEqualTo(1);
            assertThat(quality.falsePositive()).isZero();
        }

        @Test
        void AI와_관리자가_동의한_건은_뒤집힘으로_세지_않는다() {
            saveDecided("욕설닉", false, NicknameAuditStatus.BLOCKED);
            saveDecided("보통닉", true, NicknameAuditStatus.ALLOWED);

            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.total()).isEqualTo(2);
            assertThat(quality.agreed()).isEqualTo(2);
            assertThat(quality.overrideRate()).isZero();
        }

        @Test
        void 네_조합을_한_번에_집계한다() {
            saveDecided("a", false, NicknameAuditStatus.BLOCKED);
            saveDecided("b", false, NicknameAuditStatus.BLOCKED);
            saveDecided("c", false, NicknameAuditStatus.ALLOWED);
            saveDecided("d", true, NicknameAuditStatus.BLOCKED);
            saveDecided("e", true, NicknameAuditStatus.ALLOWED);

            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.total()).isEqualTo(5);
            assertThat(quality.falsePositive()).isEqualTo(1);
            assertThat(quality.falseNegative()).isEqualTo(1);
            assertThat(quality.agreed()).isEqualTo(3);
            assertThat(quality.overrideRate()).isEqualTo(0.4);
            assertThat(quality.sampleReviewed()).as("표본인 d, e만 센다.").isEqualTo(2);
        }

        @Test
        void 운영자가_아직_결정하지_않은_행은_세지_않는다() {
            save("통과닉", NicknameAuditStatus.CLEAN);
            save("걸린닉", NicknameAuditStatus.FLAGGED);
            save("애매닉", NicknameAuditStatus.PENDING);

            final NicknameAuditQuality quality = qualityStatisticsRepository.findNicknameAuditQuality(FROM, TO);

            assertThat(quality.total()).isZero();
        }

        @Test
        void 기간_밖의_판정은_세지_않는다() {
            saveDecided("a", false, NicknameAuditStatus.ALLOWED);

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
