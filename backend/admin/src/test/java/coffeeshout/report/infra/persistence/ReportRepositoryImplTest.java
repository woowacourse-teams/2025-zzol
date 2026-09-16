package coffeeshout.report.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 목록 필터는 조건을 null 로 두면 그 조건을 걸지 않는 식이라, 조건이 실제로 걸리는지는 DB
 * 에서만 확인된다. 목록 쿼리와 건수 쿼리가 <b>같은 조건</b>을 쓰는지도 여기서 갈린다. 둘이
 * 어긋나면 화면에 한 건만 뜨면서 페이지 수는 여러 장으로 찍힌다.
 */
@DisplayName("ReportRepositoryImpl.findWithFilters")
class ReportRepositoryImplTest extends AdminModuleServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

    @Autowired
    private ReportRepository reportRepository;

    private Report save(ReportCategory category, ReportStatus status) {
        final Report report = Report.createGeneralReport(category, "내용", CREATED_AT, new Reporter(1L, "ABCDE"));
        if (status == ReportStatus.RESOLVED) {
            report.resolve();
        }
        return reportRepository.save(report);
    }

    @Nested
    class 유형 {

        @Test
        void 유형을_주면_그_유형만_나온다() {
            save(ReportCategory.BUG, ReportStatus.PENDING);
            save(ReportCategory.SUGGESTION, ReportStatus.PENDING);

            final Page<Report> found = reportRepository.findWithFilters(null, ReportCategory.BUG, null, FIRST_PAGE);

            assertThat(found.getContent()).extracting(Report::getCategory).containsExactly(ReportCategory.BUG);
        }

        @Test
        void 유형을_주지_않으면_전부_나온다() {
            save(ReportCategory.BUG, ReportStatus.PENDING);
            save(ReportCategory.SUGGESTION, ReportStatus.PENDING);

            final Page<Report> found = reportRepository.findWithFilters(null, null, null, FIRST_PAGE);

            assertThat(found.getContent())
                    .extracting(Report::getCategory)
                    .containsExactlyInAnyOrder(ReportCategory.BUG, ReportCategory.SUGGESTION);
        }

        @Test
        void 건수도_같은_조건으로_센다() {
            save(ReportCategory.BUG, ReportStatus.PENDING);
            save(ReportCategory.SUGGESTION, ReportStatus.PENDING);
            save(ReportCategory.OTHER, ReportStatus.PENDING);

            final Page<Report> found = reportRepository.findWithFilters(null, ReportCategory.BUG, null, FIRST_PAGE);

            assertThat(found.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    class 유형과_상태 {

        @Test
        void 둘_다_주면_둘_다_만족하는_것만_나온다() {
            final Report target = save(ReportCategory.BUG, ReportStatus.PENDING);
            save(ReportCategory.BUG, ReportStatus.RESOLVED);
            save(ReportCategory.SUGGESTION, ReportStatus.PENDING);

            final Page<Report> found =
                    reportRepository.findWithFilters(ReportStatus.PENDING, ReportCategory.BUG, null, FIRST_PAGE);

            assertThat(found.getContent()).extracting(Report::getId).containsExactly(target.getId());
        }

        @Test
        void 조건에_맞는_것이_없으면_빈_목록이다() {
            save(ReportCategory.BUG, ReportStatus.PENDING);

            final Page<Report> found =
                    reportRepository.findWithFilters(ReportStatus.RESOLVED, ReportCategory.BUG, null, FIRST_PAGE);

            assertThat(found.getContent()).isEmpty();
            assertThat(found.getTotalElements()).isZero();
        }
    }

    @Test
    void 최근_것이_먼저_나온다() {
        final Report older = reportRepository.save(
                Report.createGeneralReport(ReportCategory.BUG, "예전", CREATED_AT, new Reporter(1L, "ABCDE")));
        final Report newer = reportRepository.save(Report.createGeneralReport(
                ReportCategory.BUG, "최근", CREATED_AT.plusSeconds(60), new Reporter(2L, "FGHIJ")));

        final Page<Report> found = reportRepository.findWithFilters(null, ReportCategory.BUG, null, FIRST_PAGE);

        assertThat(found.getContent()).extracting(Report::getId).isEqualTo(List.of(newer.getId(), older.getId()));
    }
}
