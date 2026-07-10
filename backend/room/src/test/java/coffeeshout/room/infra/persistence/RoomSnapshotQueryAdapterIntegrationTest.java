package coffeeshout.room.infra.persistence;

import coffeeshout.RoomModuleIntegrationTest;
import coffeeshout.gamecommon.RoomSnapshotQuery;
import coffeeshout.gamecommon.RoomSnapshotQuery.PlayerSnapshot;
import coffeeshout.room.domain.player.PlayerType;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link RoomSnapshotQueryAdapter}의 <b>크로스 모듈 id 공급</b>을 실제 DB로 검증한다(ADR-0034).
 *
 * <p>:game이 미니게임 결과를 {@code Long} FK로 저장하려면 room_session id와 이름별 player id·userId가 필요하다.
 * 그 공급 경로(파생 쿼리 {@code findByRoomSession_IdAndPlayerNameIn} 포함)가 TestContainer DB에서 동작하는지 확인한다.
 */
class RoomSnapshotQueryAdapterIntegrationTest extends RoomModuleIntegrationTest {

    private static final String JOIN_CODE = "S9K2";

    @Autowired
    private RoomSnapshotQuery roomSnapshotQuery;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("resolveRoomSessionId는 joinCode의 room_session id를, resolvePlayers는 이름별 playerId·userId(게스트는 null)를 반환한다")
    void 방과_플레이어_식별자를_조회한다() {
        // given — RoomEntity + 회원/게스트 PlayerEntity를 실제 DB에 커밋한다
        final TransactionTemplate tx = new TransactionTemplate(transactionManager);
        final long[] ids = new long[3]; // [roomSessionId, 한스 playerId, 루키 playerId]
        tx.executeWithoutResult(status -> {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity(JOIN_CODE));
            final PlayerEntity 한스 = playerJpaRepository.save(new PlayerEntity(room, "한스", PlayerType.HOST, 100L));
            final PlayerEntity 루키 = playerJpaRepository.save(new PlayerEntity(room, "루키", PlayerType.GUEST, null));
            ids[0] = room.getId();
            ids[1] = 한스.getId();
            ids[2] = 루키.getId();
        });

        // when
        final long roomSessionId = roomSnapshotQuery.resolveRoomSessionId(JOIN_CODE);
        final List<PlayerSnapshot> players = roomSnapshotQuery.resolvePlayers(roomSessionId, List.of("한스", "루키"));

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(roomSessionId).isEqualTo(ids[0]);
            softly.assertThat(players).containsExactlyInAnyOrder(
                    new PlayerSnapshot("한스", ids[1], 100L),
                    new PlayerSnapshot("루키", ids[2], null)
            );
        });
    }
}
