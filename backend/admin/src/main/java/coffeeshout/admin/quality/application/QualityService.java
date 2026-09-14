package coffeeshout.admin.quality.application;

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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영 품질 지표.
 *
 * <p>"일이 얼마나 밀렸나"(대기 큐)와 "잘하고 있나"(여기)는 다른 질문이다.
 * 대기 건수가 0이어도 검열 모델이 멀쩡한 닉네임을 계속 막고 있을 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualityService {

    /**
     * AI 신뢰도 구간.
     *
     * <p>0.6 아래가 두꺼우면 모델이 <b>확신 없이 걸고 있다</b>는 뜻이고, 그 구간은 사람이
     * 뒤집는 비율도 높다. 검열 기준선을 어디에 둘지 정하는 자리가 이 그래프다.
     */
    private static final List<ConfidenceBand> CONFIDENCE_BANDS = List.of(
            new ConfidenceBand("0.6 미만", 0.6),
            new ConfidenceBand("0.6~0.8", 0.8),
            new ConfidenceBand("0.8~0.9", 0.9),
            new ConfidenceBand("0.9 이상", 1.01));

    /** 사람이 봐야 하는 판정. 나머지는 그대로 지나간 것이다. */
    private static final List<NicknameAuditStatus> NEEDS_EYES =
            List.of(NicknameAuditStatus.FLAGGED, NicknameAuditStatus.PENDING, NicknameAuditStatus.BLOCKED);

    private final QualityStatisticsRepository qualityStatisticsRepository;
    private final Clock clock;

    public NicknameAuditQuality nicknameAuditQuality(int days) {
        final Instant to = clock.instant();
        return qualityStatisticsRepository.findNicknameAuditQuality(to.minus(Duration.ofDays(days)), to);
    }

    public ReportBacklog reportBacklog() {
        final Instant now = clock.instant();
        final long oldestPendingMinutes = qualityStatisticsRepository
                .findOldestPendingReportCreatedAt()
                .map(createdAt -> Duration.between(createdAt, now).toMinutes())
                .orElse(0L);

        return new ReportBacklog(qualityStatisticsRepository.countPendingReports(), oldestPendingMinutes);
    }

    /**
     * 신고 화면 상단 그래프.
     *
     * <p>세 그래프가 한 번의 조회에서 나온다. 따로 돌리면 그사이 신고 하나가 처리되어
     * 카테고리 합과 날짜 합이 어긋난다.
     */
    public ReportStats reportStats(int days) {
        final ZoneId zone = clock.getZone();
        final LocalDate today = LocalDate.now(clock);
        final LocalDate first = today.minusDays(days - 1L);
        final List<ReportRecord> reports = qualityStatisticsRepository.findReportsBetween(
                first.atStartOfDay(zone).toInstant(),
                today.plusDays(1).atStartOfDay(zone).toInstant());

        return new ReportStats(
                reports.size(),
                countCategories(reports),
                countGames(reports),
                countReportsByDate(reports, zone, first, today));
    }

    /** 신고가 없는 카테고리도 0으로 보낸다. 화면이 채우게 두면 카테고리가 늘었을 때 칸이 사라진다. */
    private static List<ReportStats.CategoryCount> countCategories(List<ReportRecord> reports) {
        final List<ReportStats.CategoryCount> counts = new ArrayList<>();
        for (ReportCategory category : ReportCategory.values()) {
            counts.add(new ReportStats.CategoryCount(
                    category,
                    reports.stream()
                            .filter(report -> report.category() == category)
                            .count()));
        }
        return counts;
    }

    /**
     * 게임별 신고. 많은 순으로 준다.
     *
     * <p>여기는 0인 게임을 빼고 보낸다. 카테고리와 달리 게임은 여덟 개가 넘고, 신고가 한
     * 건도 없는 게임은 이 화면에서 할 말이 없다. 게임별 비중 카드가 같은 규칙을 쓴다.
     */
    private static List<ReportStats.GameCount> countGames(List<ReportRecord> reports) {
        final Map<MiniGameType, Long> byGame = new LinkedHashMap<>();
        for (ReportRecord report : reports) {
            byGame.merge(report.gameType(), 1L, Long::sum);
        }
        return byGame.entrySet().stream()
                .sorted(Map.Entry.<MiniGameType, Long>comparingByValue().reversed())
                .map(entry -> new ReportStats.GameCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 접수는 접수일로, 처리는 <b>처리일</b>로 센다.
     *
     * <p>둘을 접수일로 묶으면 "오늘 처리한 건수"가 오늘 들어온 것 중 처리된 것만 세어져,
     * 어제 밀린 것을 오늘 몰아서 처리한 날이 한산해 보인다.
     */
    private static List<ReportStats.DailyCount> countReportsByDate(
            List<ReportRecord> reports, ZoneId zone, LocalDate first, LocalDate last) {
        final Map<LocalDate, long[]> counts = new LinkedHashMap<>();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            counts.put(date, new long[2]);
        }
        for (ReportRecord report : reports) {
            final long[] received = counts.get(LocalDate.ofInstant(report.createdAt(), zone));
            if (received != null) {
                received[0]++;
            }
            if (report.resolvedAt() != null) {
                final long[] resolved = counts.get(LocalDate.ofInstant(report.resolvedAt(), zone));
                if (resolved != null) {
                    resolved[1]++;
                }
            }
        }
        return counts.entrySet().stream()
                .map(entry -> new ReportStats.DailyCount(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    /**
     * 검열 화면 상단 그래프.
     *
     * <p>신뢰도 분포는 <b>걸린 닉네임만</b> 센다. 그냥 지나간 닉네임의 신뢰도는 "욕일
     * 확률이 낮다"는 뜻이라 0에 몰리고, 함께 그리면 그 덩어리가 나머지를 다 눌러 버린다.
     */
    public NicknameAuditStats nicknameAuditStats(int days) {
        final ZoneId zone = clock.getZone();
        final LocalDate today = LocalDate.now(clock);
        final LocalDate first = today.minusDays(days - 1L);
        final List<AuditRecord> audits = qualityStatisticsRepository.findAuditsBetween(
                first.atStartOfDay(zone).toInstant(),
                today.plusDays(1).atStartOfDay(zone).toInstant());

        return new NicknameAuditStats(
                audits.size(),
                countStatuses(audits),
                countAuditsByDate(audits, zone, first, today),
                countConfidences(audits));
    }

    private static List<NicknameAuditStats.StatusCount> countStatuses(List<AuditRecord> audits) {
        final List<NicknameAuditStats.StatusCount> counts = new ArrayList<>();
        for (NicknameAuditStatus status : NicknameAuditStatus.values()) {
            counts.add(new NicknameAuditStats.StatusCount(
                    status,
                    audits.stream().filter(audit -> audit.status() == status).count()));
        }
        return counts;
    }

    private static List<NicknameAuditStats.DailyCount> countAuditsByDate(
            List<AuditRecord> audits, ZoneId zone, LocalDate first, LocalDate last) {
        final Map<LocalDate, long[]> counts = new LinkedHashMap<>();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            counts.put(date, new long[2]);
        }
        for (AuditRecord audit : audits) {
            final long[] day = counts.get(LocalDate.ofInstant(audit.createdAt(), zone));
            if (day != null) {
                day[NEEDS_EYES.contains(audit.status()) ? 0 : 1]++;
            }
        }
        return counts.entrySet().stream()
                .map(entry ->
                        new NicknameAuditStats.DailyCount(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private static List<NicknameAuditStats.Bucket> countConfidences(List<AuditRecord> audits) {
        final List<BigDecimal> values = audits.stream()
                .filter(audit -> NEEDS_EYES.contains(audit.status()))
                .map(AuditRecord::confidence)
                .filter(Objects::nonNull)
                .map(AiConfidence::value)
                .toList();

        final List<NicknameAuditStats.Bucket> buckets = new ArrayList<>();
        double lower = 0;
        for (ConfidenceBand band : CONFIDENCE_BANDS) {
            final double from = lower;
            buckets.add(new NicknameAuditStats.Bucket(
                    band.label(),
                    values.stream()
                            .filter(value -> value.doubleValue() >= from && value.doubleValue() < band.upperBound())
                            .count()));
            lower = band.upperBound();
        }
        return buckets;
    }

    private record ConfidenceBand(String label, double upperBound) {}
}
