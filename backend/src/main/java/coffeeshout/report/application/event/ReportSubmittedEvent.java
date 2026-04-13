package coffeeshout.report.application.event;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;

public record ReportSubmittedEvent(
        long reportId,
        ReportCategory category,
        MiniGameType gameType,
        String joinCode,
        String content
) {
}
