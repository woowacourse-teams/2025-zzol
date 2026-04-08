package coffeeshout.report.infra.persistence;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportAdminQueryRepository {

    private static final QReport REPORT = QReport.report;

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Report> findWithFilters(
            ReportStatus status,
            ReportCategory category,
            MiniGameType gameType,
            Pageable pageable
    ) {
        final List<Report> content = queryFactory
                .selectFrom(REPORT)
                .where(statusEq(status), categoryEq(category), gameTypeEq(gameType))
                .orderBy(REPORT.createdAt.desc(), REPORT.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        final Long total = queryFactory
                .select(REPORT.count())
                .from(REPORT)
                .where(statusEq(status), categoryEq(category), gameTypeEq(gameType))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private BooleanExpression statusEq(ReportStatus status) {
        return status != null ? REPORT.status.eq(status) : null;
    }

    private BooleanExpression categoryEq(ReportCategory category) {
        return category != null ? REPORT.category.eq(category) : null;
    }

    private BooleanExpression gameTypeEq(MiniGameType gameType) {
        return gameType != null ? REPORT.gameType.eq(gameType) : null;
    }
}
