package coffeeshout.room.infra.persistence;

import coffeeshout.RoomModuleIntegrationTest;
import coffeeshout.gamecommon.MemberRouletteRecordQuery;
import coffeeshout.gamecommon.MemberRouletteRecordQuery.RouletteRecord;
import coffeeshout.room.domain.player.PlayerType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link MemberRouletteRecordQueryAdapter}의 당첨·연속 생존·참여 판수 집계를 실제 DB로 검증한다(#1794).
 *
 * <p>룰렛 결과는 저장 순서대로 created_at이 오르므로, 헬퍼를 부른 순서가 곧 오래된 판에서 최신 판 순서다.
 */
class MemberRouletteRecordQueryAdapterIntegrationTest extends RoomModuleIntegrationTest {

    private static final Long 엠제이 = 100L;
    private static final Long 한스 = 200L;
    private static final Long 게스트 = null;

    @Autowired
    private MemberRouletteRecordQuery memberRouletteRecordQuery;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private RouletteResultJpaRepository rouletteResultJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private int roomSeq;

    /** 당첨자와 나머지 참가자로 방 하나를 만들고 룰렛 결과를 한 건 저장한다. 회원이 아닌 참가자는 {@code null}. */
    private void 룰렛_한_판(Long winnerUserId, Long... otherUserIds) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("R" + (++roomSeq)));
            final PlayerEntity winner =
                    playerJpaRepository.save(new PlayerEntity(room, "당첨자", PlayerType.HOST, winnerUserId));
            for (int i = 0; i < otherUserIds.length; i++) {
                playerJpaRepository.save(new PlayerEntity(room, "참가자" + i, PlayerType.GUEST, otherUserIds[i]));
            }
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, winner, 50));
        });
    }

    @Nested
    class 연속_생존 {

        @Test
        void 최신_판부터_첫_당첨_전까지_피한_횟수를_센다() {
            룰렛_한_판(엠제이, 한스);
            룰렛_한_판(한스, 엠제이);
            룰렛_한_판(엠제이, 한스);
            룰렛_한_판(한스, 엠제이);
            룰렛_한_판(게스트, 엠제이, 한스);

            final RouletteRecord record = memberRouletteRecordQuery.findByUserId(엠제이);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(record.winCount()).isEqualTo(2);
                softly.assertThat(record.survivalStreak()).isEqualTo(2);
                softly.assertThat(record.playCount()).isEqualTo(5);
            });
        }

        @Test
        void 전부_당첨이면_연속_생존은_0이다() {
            룰렛_한_판(엠제이, 한스);
            룰렛_한_판(엠제이, 한스);
            룰렛_한_판(엠제이, 한스);

            final RouletteRecord record = memberRouletteRecordQuery.findByUserId(엠제이);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(record.winCount()).isEqualTo(3);
                softly.assertThat(record.survivalStreak()).isZero();
                softly.assertThat(record.playCount()).isEqualTo(3);
            });
        }

        @Test
        void 당첨이_없으면_연속_생존은_참여_판수와_같다() {
            룰렛_한_판(한스, 엠제이);
            룰렛_한_판(게스트, 엠제이);
            룰렛_한_판(한스, 엠제이);

            final RouletteRecord record = memberRouletteRecordQuery.findByUserId(엠제이);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(record.winCount()).isZero();
                softly.assertThat(record.survivalStreak()).isEqualTo(3);
                softly.assertThat(record.playCount()).isEqualTo(3);
            });
        }
    }

    @Nested
    class 참여_판정 {

        @Test
        void 참여하지_않은_방의_결과는_세지_않는다() {
            룰렛_한_판(한스);
            룰렛_한_판(엠제이, 한스);
            룰렛_한_판(한스);

            final RouletteRecord 엠제이_기록 = memberRouletteRecordQuery.findByUserId(엠제이);
            final RouletteRecord 한스_기록 = memberRouletteRecordQuery.findByUserId(한스);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(엠제이_기록).isEqualTo(new RouletteRecord(1, 0, 1));
                softly.assertThat(한스_기록).isEqualTo(new RouletteRecord(2, 0, 3));
            });
        }

        @Test
        void 한_방에_회원_player_행이_둘이어도_한_판으로_센다() {
            룰렛_한_판(한스, 엠제이, 엠제이);

            final RouletteRecord record = memberRouletteRecordQuery.findByUserId(엠제이);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(record.playCount()).isEqualTo(1);
                softly.assertThat(record.survivalStreak()).isEqualTo(1);
            });
        }

        @Test
        void 기록이_없으면_모두_0이다() {
            룰렛_한_판(한스);

            final RouletteRecord record = memberRouletteRecordQuery.findByUserId(엠제이);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(record.winCount()).isZero();
                softly.assertThat(record.survivalStreak()).isZero();
                softly.assertThat(record.playCount()).isZero();
            });
        }
    }
}
