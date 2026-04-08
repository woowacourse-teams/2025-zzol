package coffeeshout.room.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.repository.RouletteResultPort;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RouletteResultJpaAdapterTest extends ServiceTest {

    @Autowired
    private RouletteResultPort rouletteResultPort;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Nested
    class 방_상태_룰렛으로_변경 {

        @Test
        void 방_상태가_ROULETTE로_변경된다() {
            // given
            roomJpaRepository.save(new RoomEntity("ABCD"));

            // when
            rouletteResultPort.updateRoomStatusToRoulette("ABCD");

            // then
            final RoomEntity updated = roomJpaRepository
                    .findFirstByJoinCodeOrderByCreatedAtDesc("ABCD")
                    .orElseThrow();
            assertThat(updated.getRoomStatus()).isEqualTo(RoomState.ROULETTE);
        }
    }

    @Nested
    class 방_종료_및_룰렛_결과_저장 {

        @Test
        void 방_상태가_DONE으로_변경되고_룰렛_결과가_저장된다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            playerJpaRepository.save(new PlayerEntity(room, "한스", PlayerType.HOST));
            final Winner winner = new Winner(new PlayerName("한스"), 0, 0, 50);

            // when
            rouletteResultPort.finishRoomAndSaveResult("ABCD", winner);

            // then
            entityManager.flush();
            final RoomEntity updated = roomJpaRepository
                    .findFirstByJoinCodeOrderByCreatedAtDesc("ABCD")
                    .orElseThrow();
            final int savedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roulette_result WHERE room_session_id = ?",
                    Integer.class, room.getId()
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updated.getRoomStatus()).isEqualTo(RoomState.DONE);
                softly.assertThat(updated.isDone()).isTrue();
                softly.assertThat(savedCount).isEqualTo(1);
            });
        }

        @Test
        void 이미_DONE_상태이면_룰렛_결과를_중복_저장하지_않는다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            playerJpaRepository.save(new PlayerEntity(room, "한스", PlayerType.HOST));
            final Winner winner = new Winner(new PlayerName("한스"), 0, 0, 50);
            rouletteResultPort.finishRoomAndSaveResult("ABCD", winner);
            entityManager.flush();

            // when - 동일 룰렛 결과를 한 번 더 요청
            rouletteResultPort.finishRoomAndSaveResult("ABCD", winner);
            entityManager.flush();

            // then - 최초 1건만 저장돼 있어야 한다
            final int savedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roulette_result WHERE room_session_id = ?",
                    Integer.class, room.getId()
            );
            assertThat(savedCount).isEqualTo(1);
        }
    }
}
