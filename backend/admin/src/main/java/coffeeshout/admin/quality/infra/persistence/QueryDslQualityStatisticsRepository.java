package coffeeshout.admin.quality.infra.persistence;

import coffeeshout.admin.quality.domain.NicknameAuditQuality;
import coffeeshout.admin.quality.domain.QualityStatisticsRepository;
import coffeeshout.profanity.domain.audit.NicknameFeedback.OperatorDecision;
import coffeeshout.profanity.domain.audit.QNicknameAudit;
import coffeeshout.profanity.domain.audit.QNicknameFeedback;
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

    private static final QNicknameFeedback FEEDBACK = QNicknameFeedback.nicknameFeedback;
    private static final QReport REPORT = QReport.report;
    private static final QNicknameAudit AUDIT = QNicknameAudit.nicknameAudit;

    private final JPAQueryFactory queryFactory;

    @Override
    public NicknameAuditQuality findNicknameAuditQuality(Instant from, Instant to) {
        // AI 판정과 관리자 결정의 네 조합을 한 번에 집계한다. 조합마다 쿼리를 돌리면
        // 네 번 왕복하면서도 서로 다른 시점의 데이터를 섞어 합이 안 맞을 수 있다.
        final List<Tuple> rows = queryFactory
                .select(FEEDBACK.aiFlagged, FEEDBACK.operatorDecision, FEEDBACK.count())
                .from(FEEDBACK)
                .where(FEEDBACK.createdAt.goe(from), FEEDBACK.createdAt.lt(to))
                .groupBy(FEEDBACK.aiFlagged, FEEDBACK.operatorDecision)
                .fetch();

        long total = 0;
        long falsePositive = 0;
        long falseNegative = 0;
        for (Tuple row : rows) {
            final boolean aiFlagged = Boolean.TRUE.equals(row.get(FEEDBACK.aiFlagged));
            final OperatorDecision decision = row.get(FEEDBACK.operatorDecision);
            final long count = nullToZero(row.get(FEEDBACK.count()));

            total += count;
            if (aiFlagged && decision == OperatorDecision.ALLOWED) {
                falsePositive += count;
            }
            if (!aiFlagged && decision == OperatorDecision.BLOCKED) {
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
