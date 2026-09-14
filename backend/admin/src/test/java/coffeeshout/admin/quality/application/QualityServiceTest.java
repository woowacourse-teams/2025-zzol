package coffeeshout.admin.quality.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import coffeeshout.admin.quality.domain.NicknameAuditQuality;
import coffeeshout.admin.quality.domain.NicknameAuditStats;
import coffeeshout.admin.quality.domain.QualityStatisticsRepository;
import coffeeshout.admin.quality.domain.QualityStatisticsRepository.AuditRecord;
import coffeeshout.admin.quality.domain.QualityStatisticsRepository.ReportRecord;
import coffeeshout.admin.quality.domain.ReportBacklog;
import coffeeshout.admin.quality.domain.ReportStats;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.report.domain.ReportCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("QualityService")
@ExtendWith(MockitoExtension.class)
class QualityServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private QualityStatisticsRepository qualityStatisticsRepository;

    private QualityService service() {
        return new QualityService(qualityStatisticsRepository, CLOCK);
    }

    @Nested
    class reportBacklog {

        @Test
        void 가장_오래된_미처리_신고의_나이를_분으로_돌려준다() {
            given(qualityStatisticsRepository.findOldestPendingReportCreatedAt())
                    .willReturn(Optional.of(NOW.minusSeconds(3 * 3600)));
            given(qualityStatisticsRepository.countPendingReports()).willReturn(4L);

            final ReportBacklog backlog = service().reportBacklog();

            assertThat(backlog.oldestPendingMinutes()).isEqualTo(180);
            assertThat(backlog.pendingCount()).isEqualTo(4L);
        }

        @Test
        void 미처리_신고가_없으면_대기_시간이_0이다() {
            given(qualityStatisticsRepository.findOldestPendingReportCreatedAt())
                    .willReturn(Optional.empty());

            assertThat(service().reportBacklog().oldestPendingMinutes()).isZero();
        }
    }

    private static Instant daysAgo(long days) {
        return NOW.minusSeconds(days * 24 * 3600);
    }

    private static ReportRecord report(ReportCategory category, MiniGameType game, long createdDaysAgo) {
        return new ReportRecord(category, game, daysAgo(createdDaysAgo), null);
    }

    private static ReportRecord resolved(long createdDaysAgo, long minutesToResolve) {
        final Instant createdAt = daysAgo(createdDaysAgo);
        return new ReportRecord(ReportCategory.BUG, null, createdAt, createdAt.plusSeconds(minutesToResolve * 60));
    }

    private static AuditRecord audit(NicknameAuditStatus status, double confidence, long createdDaysAgo) {
        return new AuditRecord(status, new AiConfidence(BigDecimal.valueOf(confidence)), daysAgo(createdDaysAgo));
    }

    @Nested
    class reportStats {

        @Test
        void 신고가_없는_카테고리도_0으로_보낸다() {
            // 화면이 빠진 카테고리를 채우게 두면 카테고리가 하나 늘었을 때 그 칸이
            // 조용히 사라진다.
            given(qualityStatisticsRepository.findReportsBetween(any(), any()))
                    .willReturn(List.of(report(ReportCategory.BUG, null, 1)));

            assertThat(service().reportStats(30).categories())
                    .hasSize(ReportCategory.values().length)
                    .extracting(ReportStats.CategoryCount::category)
                    .containsExactly(ReportCategory.values());
        }

        @Test
        void 게임별_신고는_많은_순이고_게임이_없는_신고도_한_칸을_차지한다() {
            given(qualityStatisticsRepository.findReportsBetween(any(), any()))
                    .willReturn(List.of(
                            report(ReportCategory.BUG, MiniGameType.CARD_GAME, 1),
                            report(ReportCategory.BUG, MiniGameType.CARD_GAME, 2),
                            report(ReportCategory.SUGGESTION, null, 1),
                            report(ReportCategory.BUG, MiniGameType.RACING_GAME, 3)));

            assertThat(service().reportStats(30).games())
                    .extracting(ReportStats.GameCount::gameType, ReportStats.GameCount::count)
                    .containsExactly(
                            tuple(MiniGameType.CARD_GAME, 2L), tuple(null, 1L), tuple(MiniGameType.RACING_GAME, 1L));
        }

        @Test
        void 처리는_접수일이_아니라_처리일로_센다() {
            // 둘을 접수일로 묶으면 어제 밀린 것을 오늘 몰아서 처리한 날이 한산해 보인다.
            //
            // 사흘 전에 들어와 <b>이틀 뒤</b>에 처리된 신고 하나를 둔다. 날을 넘기지 않는
            // 신고로 시험하면 접수일로 세든 처리일로 세든 같은 칸에 들어가 아무것도 재지 못한다.
            given(qualityStatisticsRepository.findReportsBetween(any(), any()))
                    .willReturn(List.of(resolved(3, 2 * 24 * 60)));

            final List<ReportStats.DailyCount> daily = service().reportStats(30).daily();

            assertThat(daily)
                    .filteredOn(point -> point.received() > 0 || point.resolved() > 0)
                    .extracting(
                            ReportStats.DailyCount::date,
                            ReportStats.DailyCount::received,
                            ReportStats.DailyCount::resolved)
                    .containsExactly(tuple(LocalDate.of(2026, 9, 3), 1L, 0L), tuple(LocalDate.of(2026, 9, 5), 0L, 1L));
        }

        @Test
        void 접수가_없는_날도_0으로_채운다() {
            given(qualityStatisticsRepository.findReportsBetween(any(), any())).willReturn(List.of());

            assertThat(service().reportStats(7).daily())
                    .hasSize(7)
                    .extracting(ReportStats.DailyCount::date)
                    .endsWith(LocalDate.of(2026, 9, 6));
        }
    }

    @Nested
    class nicknameAuditStats {

        @Test
        void 사람이_봐야_하는_판정만_걸린_것으로_센다() {
            given(qualityStatisticsRepository.findAuditsBetween(any(), any()))
                    .willReturn(List.of(
                            audit(NicknameAuditStatus.FLAGGED, 0.9, 1),
                            audit(NicknameAuditStatus.PENDING, 0.7, 1),
                            audit(NicknameAuditStatus.BLOCKED, 0.95, 1),
                            audit(NicknameAuditStatus.CLEAN, 0.1, 1),
                            audit(NicknameAuditStatus.ALLOWED, 0.5, 1)));

            final List<NicknameAuditStats.DailyCount> daily =
                    service().nicknameAuditStats(30).daily();

            assertThat(daily.stream()
                            .mapToLong(NicknameAuditStats.DailyCount::flagged)
                            .sum())
                    .isEqualTo(3);
            assertThat(daily.stream()
                            .mapToLong(NicknameAuditStats.DailyCount::passed)
                            .sum())
                    .isEqualTo(2);
        }

        @Test
        void 신뢰도_분포는_걸린_닉네임만_센다() {
            // 그냥 지나간 닉네임의 신뢰도는 0 근처에 몰려, 함께 그리면 그 덩어리가
            // 나머지를 다 눌러 버린다.
            given(qualityStatisticsRepository.findAuditsBetween(any(), any()))
                    .willReturn(List.of(
                            audit(NicknameAuditStatus.FLAGGED, 0.95, 1),
                            audit(NicknameAuditStatus.CLEAN, 0.05, 1),
                            audit(NicknameAuditStatus.CLEAN, 0.02, 1)));

            final List<NicknameAuditStats.Bucket> buckets =
                    service().nicknameAuditStats(30).confidenceBuckets();

            assertThat(buckets.stream()
                            .mapToLong(NicknameAuditStats.Bucket::count)
                            .sum())
                    .isEqualTo(1);
            assertThat(buckets.getLast().count()).isEqualTo(1);
        }

        @Test
        void 신뢰도_구간은_아래를_포함하고_위를_뺀다() {
            given(qualityStatisticsRepository.findAuditsBetween(any(), any()))
                    .willReturn(List.of(
                            audit(NicknameAuditStatus.FLAGGED, 0.5, 1),
                            audit(NicknameAuditStatus.FLAGGED, 0.6, 1),
                            audit(NicknameAuditStatus.FLAGGED, 0.8, 1),
                            audit(NicknameAuditStatus.FLAGGED, 0.9, 1),
                            audit(NicknameAuditStatus.FLAGGED, 1.0, 1)));

            assertThat(service().nicknameAuditStats(30).confidenceBuckets())
                    .extracting(NicknameAuditStats.Bucket::label, NicknameAuditStats.Bucket::count)
                    .containsExactly(
                            tuple("0.6 미만", 1L), tuple("0.6~0.8", 1L), tuple("0.8~0.9", 1L), tuple("0.9 이상", 2L));
        }

        @Test
        void 하나도_없는_판정도_0으로_보낸다() {
            given(qualityStatisticsRepository.findAuditsBetween(any(), any()))
                    .willReturn(List.of(audit(NicknameAuditStatus.FLAGGED, 0.9, 1)));

            assertThat(service().nicknameAuditStats(30).statuses())
                    .hasSize(NicknameAuditStatus.values().length)
                    .extracting(NicknameAuditStats.StatusCount::status)
                    .containsExactly(NicknameAuditStatus.values());
        }
    }

    @Nested
    class nicknameAuditQuality {

        @Test
        void 조회_기간을_시계_기준으로_계산한다() {
            given(qualityStatisticsRepository.findNicknameAuditQuality(NOW.minusSeconds(30 * 24 * 3600), NOW))
                    .willReturn(new NicknameAuditQuality(100, 10, 5));

            assertThat(service().nicknameAuditQuality(30).total()).isEqualTo(100);
        }
    }
}
