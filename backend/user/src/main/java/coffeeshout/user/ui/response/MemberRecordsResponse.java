package coffeeshout.user.ui.response;

import coffeeshout.gamecommon.MemberMiniGameRecordQuery.GameRecord;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery.MiniGameRecords;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery.MostPlayed;
import coffeeshout.gamecommon.MemberRouletteRecordQuery.RouletteRecord;
import coffeeshout.user.application.service.MemberRecordService.MemberRecords;
import java.util.List;

/** 응답에 userId를 싣지 않는다(ADR-0024). */
public record MemberRecordsResponse(RouletteRecord roulette, MinigameSummary minigame, List<GameRecord> games) {

    public record MinigameSummary(int totalPlayCount, MostPlayed mostPlayed) {}

    public static MemberRecordsResponse from(MemberRecords records) {
        final MiniGameRecords minigame = records.minigame();
        return new MemberRecordsResponse(
                records.roulette(),
                new MinigameSummary(minigame.totalPlayCount(), minigame.mostPlayed()),
                minigame.games());
    }
}
