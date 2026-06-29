package coffeeshout.nunchi.ui.response;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.nunchi.domain.NunchiScore;
import coffeeshout.nunchi.domain.NunchiTier;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 눈치게임 결과 응답(ADR-0031 N7). 공유 결과 DTO는 {@code tier}가 없어 같은 rank의 동점 그룹이 충돌인지
 * 미입력인지 FE가 구분할 수 없으므로, nunchi 전용 응답에 계층을 함께 노출한다. rank는
 * {@link MiniGameResult} standard-competition(1,2,2,4,6), tier는 {@link NunchiScore} 밴드에서 도출.
 */
public record NunchiResultResponse(List<Entry> results) {

    public record Entry(String playerName, int rank, NunchiTier tier) {
    }

    public static NunchiResultResponse of(MiniGameResult result, Map<Gamer, MiniGameScore> scores) {
        final List<Entry> entries = scores.entrySet().stream()
                .map(e -> new Entry(
                        e.getKey().getName(),
                        result.getPlayerRank(e.getKey()),
                        ((NunchiScore) e.getValue()).getTier()))
                .sorted(Comparator.comparingInt(Entry::rank)) // rank 오름차순
                .toList();
        return new NunchiResultResponse(entries);
    }
}
