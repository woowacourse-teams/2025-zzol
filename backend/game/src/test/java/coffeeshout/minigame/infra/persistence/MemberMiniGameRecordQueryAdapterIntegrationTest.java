package coffeeshout.minigame.infra.persistence;

import static coffeeshout.minigame.domain.MiniGameType.BLIND_TIMER;
import static coffeeshout.minigame.domain.MiniGameType.BLOCK_STACKING;
import static coffeeshout.minigame.domain.MiniGameType.CARD_GAME;
import static coffeeshout.minigame.domain.MiniGameType.RACING_GAME;
import static coffeeshout.minigame.domain.MiniGameType.SPEED_TOUCH;

import coffeeshout.GameModuleIntegrationTest;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery.GameRecord;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery.MiniGameRecords;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery.MostPlayed;
import coffeeshout.minigame.domain.MiniGameType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link MemberMiniGameRecordQueryAdapter}의 회원별 미니게임 집계를 실제 DB로 검증한다(#1794).
 *
 * <p>테스트 DB는 {@code ddl-auto: create}라 player FK가 없다. player_id는 임의 값이다.
 */
class MemberMiniGameRecordQueryAdapterIntegrationTest extends GameModuleIntegrationTest {

    private static final Long 엠제이 = 7001L;
    private static final Long 한스 = 7002L;
    private static final Long 게스트 = null;
    private static final long DNF = 1_000_000L;
    private static final long 타임아웃 = 999_999_999L;

    @Autowired
    private MemberMiniGameRecordQuery memberMiniGameRecordQuery;

    @Autowired
    private MiniGameJpaRepository miniGameJpaRepository;

    @Autowired
    private MiniGameResultJpaRepository miniGameResultJpaRepository;

    private long roomSessionSeq;

    @BeforeEach
    void 두_회원과_게스트의_결과를_넣는다() {
        결과_저장(RACING_GAME, 엠제이, 12_000, 15_000, DNF);
        결과_저장(BLOCK_STACKING, 엠제이, 14, 9);
        결과_저장(BLIND_TIMER, 엠제이, 120, 타임아웃);
        결과_저장(CARD_GAME, 엠제이, 1, 2, 3);

        결과_저장(RACING_GAME, 한스, DNF, DNF);
        결과_저장(BLIND_TIMER, 한스, 300);

        결과_저장(RACING_GAME, 게스트, 500);
        결과_저장(SPEED_TOUCH, 게스트, 700);
    }

    private void 결과_저장(MiniGameType type, Long userId, long... scores) {
        final MiniGameEntity play = miniGameJpaRepository.save(new MiniGameEntity(++roomSessionSeq, type));
        for (long score : scores) {
            miniGameResultJpaRepository.save(new MiniGameResultEntity(play, 1L, 1, score, userId));
        }
    }

    @Test
    void 네_게임의_완주_기록을_고정_순서로_집계하고_DNF와_타회원은_뺀다() {
        final MiniGameRecords records = memberMiniGameRecordQuery.findByUserId(엠제이);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(records.games())
                    .containsExactly(
                            new GameRecord(RACING_GAME, 2, 12_000L, 13_500L),
                            new GameRecord(BLOCK_STACKING, 2, 14L, 12L),
                            new GameRecord(BLIND_TIMER, 1, 120L, 120L),
                            new GameRecord(SPEED_TOUCH, 0, null, null));
            softly.assertThat(records.totalPlayCount()).as("8종 전체, DNF 포함").isEqualTo(10);
            softly.assertThat(records.mostPlayed())
                    .as("레이싱과 카드게임이 3판 동률이면 enum 순서가 앞선 카드게임")
                    .isEqualTo(new MostPlayed(CARD_GAME, 3));
        });
    }

    @Test
    void 전부_DNF인_게임은_판수만_전체에_들어가고_기록은_비어_있다() {
        final MiniGameRecords records = memberMiniGameRecordQuery.findByUserId(한스);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(records.games())
                    .containsExactly(
                            new GameRecord(RACING_GAME, 0, null, null),
                            new GameRecord(BLOCK_STACKING, 0, null, null),
                            new GameRecord(BLIND_TIMER, 1, 300L, 300L),
                            new GameRecord(SPEED_TOUCH, 0, null, null));
            softly.assertThat(records.totalPlayCount()).isEqualTo(3);
            softly.assertThat(records.mostPlayed()).isEqualTo(new MostPlayed(RACING_GAME, 2));
        });
    }

    @Test
    void 기록이_없는_회원은_판수_0과_null_기록을_받는다() {
        final MiniGameRecords records = memberMiniGameRecordQuery.findByUserId(9999L);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(records.totalPlayCount()).isZero();
            softly.assertThat(records.mostPlayed()).isNull();
            softly.assertThat(records.games())
                    .extracting(GameRecord::type)
                    .containsExactly(RACING_GAME, BLOCK_STACKING, BLIND_TIMER, SPEED_TOUCH);
            softly.assertThat(records.games()).allSatisfy(game -> {
                softly.assertThat(game.playCount()).isZero();
                softly.assertThat(game.best()).isNull();
                softly.assertThat(game.average()).isNull();
            });
        });
    }
}
