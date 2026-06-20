package coffeeshout.room.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.RoomModuleServiceTest;
import coffeeshout.gamecommon.PlayerRef;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerType;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("RoomReferenceAdapter")
class RoomReferenceAdapterTest extends RoomModuleServiceTest {

    @Autowired
    private RoomReferenceAdapter roomReferenceAdapter;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Nested
    @DisplayName("findCurrentRoomSessionId는")
    class FindCurrentRoomSessionId {

        @Test
        @DisplayName("joinCode에 해당하는 RoomSession의 ID를 반환한다")
        void 방_세션_ID를_반환한다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));

            final Optional<Long> result = roomReferenceAdapter.findCurrentRoomSessionId("ABCD");

            assertThat(result).contains(room.getId());
        }

        @Test
        @DisplayName("존재하지 않는 joinCode면 빈 Optional을 반환한다")
        void 존재하지_않으면_빈_Optional을_반환한다() {
            final Optional<Long> result = roomReferenceAdapter.findCurrentRoomSessionId("ZZZZ");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("markRoomPlaying은")
    class MarkRoomPlaying {

        @Test
        @DisplayName("RoomSession의 상태를 PLAYING으로 전이한다")
        void 상태를_PLAYING으로_전이한다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("BCDE"));
            assertThat(room.getRoomStatus()).isEqualTo(RoomState.READY);

            roomReferenceAdapter.markRoomPlaying("BCDE");

            final RoomEntity reloaded = roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc("BCDE")
                    .orElseThrow();
            assertThat(reloaded.getRoomStatus()).isEqualTo(RoomState.PLAYING);
        }

        @Test
        @DisplayName("존재하지 않는 joinCode면 예외 없이 무시한다")
        void 존재하지_않으면_무시한다() {
            roomReferenceAdapter.markRoomPlaying("ZZZZ");
        }
    }

    @Nested
    @DisplayName("findPlayerRefs는")
    class FindPlayerRefs {

        @Test
        @DisplayName("roomSessionId와 이름이 일치하는 플레이어의 id·name·userId를 반환한다")
        void 일치하는_플레이어_참조를_반환한다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("CDEF"));
            final PlayerEntity 한스 = playerJpaRepository.save(new PlayerEntity(room, "한스", PlayerType.HOST, 1L));
            final PlayerEntity 루키 = playerJpaRepository.save(new PlayerEntity(room, "루키", PlayerType.GUEST, null));

            final List<PlayerRef> refs = roomReferenceAdapter.findPlayerRefs(room.getId(), List.of("한스", "루키"));

            assertThat(refs).containsExactlyInAnyOrder(
                    new PlayerRef(한스.getId(), "한스", 1L),
                    new PlayerRef(루키.getId(), "루키", null)
            );
        }

        @Test
        @DisplayName("이름 목록에 없는 플레이어는 제외한다")
        void 이름에_없는_플레이어는_제외한다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("DEFG"));
            playerJpaRepository.save(new PlayerEntity(room, "한스", PlayerType.HOST, 1L));
            playerJpaRepository.save(new PlayerEntity(room, "미선택", PlayerType.GUEST, 2L));

            final List<PlayerRef> refs = roomReferenceAdapter.findPlayerRefs(room.getId(), List.of("한스"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(refs).hasSize(1);
                softly.assertThat(refs.getFirst().name()).isEqualTo("한스");
            });
        }

        @Test
        @DisplayName("다른 RoomSession의 동일 이름 플레이어는 제외한다")
        void 다른_방의_동일_이름은_제외한다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("EFGH"));
            final RoomEntity otherRoom = roomJpaRepository.save(new RoomEntity("FGHI"));
            final PlayerEntity 한스 = playerJpaRepository.save(new PlayerEntity(room, "한스", PlayerType.HOST, 1L));
            playerJpaRepository.save(new PlayerEntity(otherRoom, "한스", PlayerType.HOST, 9L));

            final List<PlayerRef> refs = roomReferenceAdapter.findPlayerRefs(room.getId(), List.of("한스"));

            assertThat(refs).containsExactly(new PlayerRef(한스.getId(), "한스", 1L));
        }
    }
}
