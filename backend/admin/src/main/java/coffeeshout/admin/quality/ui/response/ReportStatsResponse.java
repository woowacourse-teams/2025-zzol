package coffeeshout.admin.quality.ui.response;

import coffeeshout.admin.quality.domain.ReportStats;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import java.time.LocalDate;
import java.util.List;

/**
 * 신고 화면 상단 그래프.
 *
 * <p>게임 이름은 한글까지 붙여 내려보낸다. 홈의 게임별 비중과 같은 규칙이다. 프론트가
 * enum 이름과 한글 이름의 대응표를 따로 들면 게임이 늘었을 때 빠진 항목이 영문 그대로
 * 노출된다.
 */
public record ReportStatsResponse(
        long total, List<CategorySlice> categories, List<GameSlice> games, List<DailyPoint> daily) {

    public static ReportStatsResponse from(ReportStats stats) {
        return new ReportStatsResponse(
                stats.total(),
                stats.categories().stream()
                        .map(row -> new CategorySlice(row.category(), row.count()))
                        .toList(),
                stats.games().stream()
                        .map(row -> new GameSlice(
                                row.gameType(), row.gameType() == null ? null : row.gameType().label, row.count()))
                        .toList(),
                stats.daily().stream()
                        .map(row -> new DailyPoint(row.date(), row.received(), row.resolved()))
                        .toList());
    }

    public record CategorySlice(ReportCategory category, long count) {}

    /** @param gameType null 이면 게임과 무관한 신고다. label 도 함께 null 이다 */
    public record GameSlice(MiniGameType gameType, String label, long count) {}

    public record DailyPoint(LocalDate date, long received, long resolved) {}
}
