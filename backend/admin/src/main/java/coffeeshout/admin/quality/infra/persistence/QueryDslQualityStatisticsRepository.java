package coffeeshout.admin.quality.infra.persistence;

import coffeeshout.admin.quality.domain.NicknameAuditQuality;
import coffeeshout.admin.quality.domain.QualityStatisticsRepository;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.QNicknameAudit;
import coffeeshout.report.domain.ReportStatus;
import coffeeshout.report.infra.persistence.QReport;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueryDslQualityStatisticsRepository implements QualityStatisticsRepository {

    private static final QReport REPORT = QReport.report;
    private static final QNicknameAudit AUDIT = QNicknameAudit.nicknameAudit;

    private final JPAQueryFactory queryFactory;

    /**
     * 운영자가 결정한 감사 행(ALLOWED·BLOCKED)을 표본 여부와 함께 센다.
     *
     * <p>표본이 아닌 행이 ALLOWED·BLOCKED가 되는 건 AI가 FLAGGED·PENDING으로 걸렀을 때뿐이라, 그중 ALLOWED는
     * 오탐이다. 표본은 AI가 CLEAN으로 통과시킨 행이라 BLOCKED면 미탐이다. 기간은 AI 판정 시각으로 자른다.
     */
    @Override
    public NicknameAuditQuality findNicknameAuditQuality(Instant from, Instant to) {
        // 조합마다 쿼리를 돌리면 서로 다른 시점의 데이터를 섞어 합이 안 맞을 수 있어 한 번에 집계한다.
        final List<Tuple> rows = queryFactory
                .select(AUDIT.reviewSample, AUDIT.status, AUDIT.count())
                .from(AUDIT)
                .where(
                        AUDIT.status.in(NicknameAuditStatus.ALLOWED, NicknameAuditStatus.BLOCKED),
                        AUDIT.auditedAt.goe(from),
                        AUDIT.auditedAt.lt(to))
                .groupBy(AUDIT.reviewSample, AUDIT.status)
                .fetch();

        long total = 0;
        long falsePositive = 0;
        long falseNegative = 0;
        for (Tuple row : rows) {
            final boolean reviewSample = Boolean.TRUE.equals(row.get(AUDIT.reviewSample));
            final NicknameAuditStatus status = row.get(AUDIT.status);
            final long count = nullToZero(row.get(AUDIT.count()));

            total += count;
            if (!reviewSample && status == NicknameAuditStatus.ALLOWED) {
                falsePositive += count;
            }
            if (reviewSample && status == NicknameAuditStatus.BLOCKED) {
                falseNegative += count;
            }
        }
        return new NicknameAuditQuality(total, falsePositive, falseNegative);
    }

    @Override
    public long countPendingReports() {
        return nullToZero(queryFactory
                .select(REPORT.count())
                .from(REPORT)
                .where(REPORT.status.eq(ReportStatus.PENDING))
                .fetchOne());
    }

    @Override
    public Optional<Instant> findOldestPendingReportCreatedAt() {
        return Optional.ofNullable(queryFactory
                .select(REPORT.createdAt.min())
                .from(REPORT)
                .where(REPORT.status.eq(ReportStatus.PENDING))
                .fetchOne());
    }

    @Override
    public List<ReportRecord> findReportsBetween(Instant from, Instant to) {
        return queryFactory
                .select(Projections.constructor(
                        ReportRecord.class, REPORT.category, REPORT.gameType, REPORT.createdAt, REPORT.resolvedAt))
                .from(REPORT)
                .where(REPORT.createdAt.goe(from), REPORT.createdAt.lt(to))
                .fetch();
    }

    /**
     * 처리일 기준 집계는 접수일로 자른 목록에 없는 신고를 포함해야 한다. 어제 접수돼 오늘
     * 처리된 신고가 그렇다. 그래서 <b>처리 시각이 기간 안</b>인 것도 함께 읽는다.
     */
    @Override
    public List<AuditRecord> findAuditsBetween(Instant from, Instant to) {
        return queryFactory
                .select(Projections.constructor(AuditRecord.class, AUDIT.status, AUDIT.confidence, AUDIT.createdAt))
                .from(AUDIT)
                .where(AUDIT.createdAt.goe(from), AUDIT.createdAt.lt(to))
                .fetch();
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
