package coffeeshout.fixture;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.ReportEntity;

public final class ReportFixture {

    private ReportFixture() {
    }

    public static ReportEntity 버그_카드게임_신고() {
        return ReportEntity.create(ReportCategory.BUG, MiniGameType.CARD_GAME, "ABC12", "카드게임이 멈춰요.");
    }

    public static ReportEntity 건의사항() {
        return ReportEntity.create(ReportCategory.SUGGESTION, null, null, "새 게임을 추가해주세요.");
    }
}
