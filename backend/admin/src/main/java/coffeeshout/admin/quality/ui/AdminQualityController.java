package coffeeshout.admin.quality.ui;

import coffeeshout.admin.quality.application.QualityService;
import coffeeshout.admin.quality.ui.response.NicknameAuditQualityResponse;
import coffeeshout.admin.quality.ui.response.NicknameAuditStatsResponse;
import coffeeshout.admin.quality.ui.response.ReportBacklogResponse;
import coffeeshout.admin.quality.ui.response.ReportStatsResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영 품질 지표. 신고 화면과 검열 화면 상단에 각각 붙는다.
 */
@RestController
@RequestMapping("/admin/api/quality")
@Validated
@RequiredArgsConstructor
public class AdminQualityController {

    private final QualityService qualityService;

    /**
     * @param days 조회 기간. 상한을 두는 것은 기간을 늘려 전체 스캔을 유발하는 것을 막기 위해서다.
     */
    @GetMapping("/nickname-audit")
    public NicknameAuditQualityResponse nicknameAudit(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return NicknameAuditQualityResponse.from(qualityService.nicknameAuditQuality(days));
    }

    /** 기간 인자가 없다. 미처리는 지금 쌓여 있는 것이고 거기에 기간은 뜻이 없다. */
    @GetMapping("/report-backlog")
    public ReportBacklogResponse reportBacklog() {
        return ReportBacklogResponse.from(qualityService.reportBacklog());
    }

    /**
     * 신고 화면 상단 그래프.
     *
     * <p>{@code /report-backlog} 와 나눠 둔다. 미처리 건수는 처리할 때마다 바뀌어 조치 후 다시 부르고,
     * 이쪽은 하루에 몇 번 안 바뀐다. 묶으면 신고 하나 처리할 때마다 기간 전체를 다시 센다.
     *
     * @param days 상한을 둔다. 기간 안의 신고를 한 줄씩 읽으므로 기간이 곧 읽는 양이다.
     */
    @GetMapping("/report-stats")
    public ReportStatsResponse reportStats(@RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        return ReportStatsResponse.from(qualityService.reportStats(days));
    }

    @GetMapping("/nickname-audit-stats")
    public NicknameAuditStatsResponse nicknameAuditStats(@RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        return NicknameAuditStatsResponse.from(qualityService.nicknameAuditStats(days));
    }
}
