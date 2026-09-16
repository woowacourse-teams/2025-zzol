package coffeeshout.report.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import coffeeshout.admin.support.PageResponse;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.ReportAdminService;
import coffeeshout.report.application.ReportAdminService.ReportRow;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import coffeeshout.report.ui.response.ReportResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("AdminReportController")
@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 6, 12, 0);

    @Mock
    private ReportAdminService reportAdminService;

    @InjectMocks
    private AdminReportController adminReportController;

    private static ReportRow row(Long id, ReportStatus status) {
        return new ReportRow(
                id, ReportCategory.BUG, MiniGameType.RACING_GAME, "ABCD", "내용", status, CREATED_AT, null, "1.2.3.4");
    }

    @Nested
    class list {

        @Test
        void 필터를_그대로_서비스에_넘긴다() {
            given(reportAdminService.list(ReportStatus.PENDING, ReportCategory.BUG, MiniGameType.RACING_GAME, 2))
                    .willReturn(new PageImpl<>(List.of(row(1L, ReportStatus.PENDING))));

            adminReportController.list(ReportStatus.PENDING, ReportCategory.BUG, MiniGameType.RACING_GAME, 2);

            then(reportAdminService)
                    .should()
                    .list(ReportStatus.PENDING, ReportCategory.BUG, MiniGameType.RACING_GAME, 2);
        }

        @Test
        void 필터가_없으면_null로_넘겨_전체를_조회한다() {
            given(reportAdminService.list(null, null, null, 0)).willReturn(new PageImpl<>(List.of()));

            adminReportController.list(null, null, null, 0);

            then(reportAdminService).should().list(null, null, null, 0);
        }

        @Test
        void 페이지_메타데이터를_함께_돌려준다() {
            given(reportAdminService.list(null, null, null, 0))
                    .willReturn(new PageImpl<>(List.of(row(1L, ReportStatus.PENDING)), PageRequest.of(0, 20), 41));

            final PageResponse<ReportResponse> response = adminReportController.list(null, null, null, 0);

            assertThat(response.totalElements()).isEqualTo(41);
            assertThat(response.totalPages()).isEqualTo(3);
            assertThat(response.content()).extracting(ReportResponse::id).containsExactly(1L);
        }
    }

    @Nested
    class pendingCount {

        @Test
        void 미처리_건수를_돌려준다() {
            // 홈 대시보드의 처리 대기 큐가 목록을 받아 세지 않아도 되게 한다.
            given(reportAdminService.countPending()).willReturn(7L);

            assertThat(adminReportController.pendingCount()).isEqualTo(7L);
        }
    }

    @Nested
    class 조치 {

        @Test
        void 처리_완료를_위임한다() {
            adminReportController.resolve(1L);

            then(reportAdminService).should().resolve(1L);
        }

        @Test
        void 신고자_IP_차단_해제를_위임한다() {
            adminReportController.unblockReporterIp(1L);

            then(reportAdminService).should().unblockReporterIp(1L);
        }
    }
}
