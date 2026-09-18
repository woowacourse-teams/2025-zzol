package coffeeshout.user.application.service;

import coffeeshout.gamecommon.MemberMiniGameRecordQuery;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery.MiniGameRecords;
import coffeeshout.gamecommon.MemberRouletteRecordQuery;
import coffeeshout.gamecommon.MemberRouletteRecordQuery.RouletteRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 내 기록 화면(#1794). 룰렛 통계는 {@code :room}, 미니게임 기록은 {@code :game}이 포트로 공급한다. */
@Service
@RequiredArgsConstructor
public class MemberRecordService {

    private final MemberRouletteRecordQuery rouletteRecordQuery;
    private final MemberMiniGameRecordQuery miniGameRecordQuery;

    public MemberRecords getRecords(long userId) {
        return new MemberRecords(rouletteRecordQuery.findByUserId(userId), miniGameRecordQuery.findByUserId(userId));
    }

    public record MemberRecords(RouletteRecord roulette, MiniGameRecords minigame) {}
}
