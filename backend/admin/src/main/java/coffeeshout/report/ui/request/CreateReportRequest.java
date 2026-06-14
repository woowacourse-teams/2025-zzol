package coffeeshout.report.ui.request;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull ReportCategory category,
        MiniGameType gameType,
        @Size(max = 10) String joinCode,
        @NotBlank @Size(max = 200) String content
) {
}
