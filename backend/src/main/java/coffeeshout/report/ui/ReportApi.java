package coffeeshout.report.ui;

import coffeeshout.report.ui.request.CreateReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

@Tag(name = "Report", description = "건의사항/신고 API")
public interface ReportApi {

    @Operation(summary = "건의사항/신고 제출", description = "카테고리별 건의사항 또는 버그 신고를 제출합니다.")
    ResponseEntity<Void> submit(CreateReportRequest request, HttpServletRequest httpRequest);
}
