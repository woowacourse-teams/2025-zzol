package coffeeshout.minigame.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.repository.MiniGamePersistence;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MiniGameJpaPersistenceTest extends ServiceTest {

    @Autowired
    private MiniGamePersistence miniGamePersistence;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private MiniGameJpaRepository miniGameJpaRepository;

    @Nested
    class 미니게임_시작_저장 {

        @Test
        void 미니게임_엔티티가_저장되고_방_상태가_PLAYING으로_변경된다() {
            // given
            roomJpaRepository.save(new RoomEntity("ABCD"));

            // when
            miniGamePersistence.saveGameStart("ABCD", MiniGameType.CARD_GAME);

            // then
            final RoomEntity updatedRoom = roomJpaRepository
                    .findFirstByJoinCodeOrderByCreatedAtDesc("ABCD")
                    .orElseThrow();
            final Optional<MiniGameEntity> savedGame = miniGameJpaRepository
                    .findByRoomSessionAndMiniGameType(updatedRoom, MiniGameType.CARD_GAME);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedRoom.getRoomStatus()).isEqualTo(RoomState.PLAYING);
                softly.assertThat(savedGame).isPresent();
                softly.assertThat(savedGame.get().getMiniGameType()).isEqualTo(MiniGameType.CARD_GAME);
            });
        }

        @Test
        void 서로_다른_미니게임_타입은_각각_별도로_저장된다() {
            // given
            roomJpaRepository.save(new RoomEntity("ABCD"));

            // when
            miniGamePersistence.saveGameStart("ABCD", MiniGameType.CARD_GAME);
            miniGamePersistence.saveGameStart("ABCD", MiniGameType.RACING_GAME);

            // then
            final RoomEntity room = roomJpaRepository
                    .findFirstByJoinCodeOrderByCreatedAtDesc("ABCD")
                    .orElseThrow();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(
                        miniGameJpaRepository.findByRoomSessionAndMiniGameType(room, MiniGameType.CARD_GAME)
                ).isPresent();
                softly.assertThat(
                        miniGameJpaRepository.findByRoomSessionAndMiniGameType(room, MiniGameType.RACING_GAME)
                ).isPresent();
            });
        }
    }
}
