package coffeeshout.admin.quality.domain;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import java.time.LocalDate;
import java.util.List;

/**
 * 신고 화면 상단의 그래프 재료.
 *
 * <p>목록은 "이 신고가 무엇인가"에 답하고 미처리 타일은 "지금 밀렸나"에 답한다.
 * 둘 다 답하지 못하는 질문이 하나 남는다. <b>무엇 때문에 신고가 들어오는가.</b> 신고의
 * 절반이 한 게임에서 나온다면 그건 신고 처리로 풀 일이 아니라 그 게임을 고칠 일이다.
 *
 * @param total          기간 안에 접수된 신고
 * @param categories     카테고리별 신고 수. 신고가 없는 카테고리도 0으로 보낸다
 * @param games          게임별 신고 수. 게임과 무관한 신고는 {@code gameType} 이 null 이다
 * @param daily          일자별 접수와 처리. 접수가 없는 날도 0으로 채운다
 */
public record ReportStats(long total, List<CategoryCount> categories, List<GameCount> games, List<DailyCount> daily) {

    public record CategoryCount(ReportCategory category, long count) {}

    /** @param gameType null 이면 게임과 무관한 신고다. 화면이 "게임 아님"으로 적는다 */
    public record GameCount(MiniGameType gameType, long count) {}

    /**
     * @param received 그날 접수된 신고
     * @param resolved 그날 <b>처리된</b> 신고. 접수일이 아니라 처리일로 센다. 둘을 같은 날로
     *                 묶으면 어제 들어와 오늘 처리한 신고가 어느 쪽에도 안 잡힌다
     */
    public record DailyCount(LocalDate date, long received, long resolved) {}
}
