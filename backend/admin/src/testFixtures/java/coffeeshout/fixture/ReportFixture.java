package coffeeshout.fixture;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.domain.ReportCategory;
import java.time.Instant;

public final class ReportFixture {

    private ReportFixture() {
    }

    public static Report 버그_카드게임_신고() {
        return Report.createBugReport(MiniGameType.CARD_GAME, "ABC12", "카드게임이 멈춰요.", Instant.now());
    }

    public static Report 건의사항() {
        return Report.createGeneralReport(ReportCategory.SUGGESTION, "새 게임을 추가해주세요.", Instant.now());
    }
}
