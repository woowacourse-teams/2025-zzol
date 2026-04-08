package coffeeshout.room.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.domain.repository.PlayerSavePort;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PlayerSaveJpaAdapterTest extends ServiceTest {

    @Autowired
    private PlayerSavePort playerSavePort;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Nested
    class 플레이어_배치_저장 {

        @Test
        void 플레이어_목록이_모두_저장된다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            final List<Player> players = List.of(
                    PlayerFixture.호스트한스(),
                    PlayerFixture.게스트꾹이(),
                    PlayerFixture.게스트루키()
            );

            // when
            playerSavePort.saveAll("ABCD", players);

            // then
            final List<PlayerEntity> saved = playerJpaRepository.findAllByRoomSession(room);
            assertThat(saved).hasSize(3);
        }

        @Test
        void 저장된_플레이어의_필드가_도메인_객체와_일치한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            final Player 한스 = PlayerFixture.호스트한스();
            final Player 꾹이 = PlayerFixture.게스트꾹이();

            // when
            playerSavePort.saveAll("ABCD", List.of(한스, 꾹이));

            // then - MapStruct 매핑 결과 검증
            final List<PlayerEntity> saved = playerJpaRepository.findAllByRoomSession(room);
            final PlayerEntity 한스Entity = saved.stream()
                    .filter(p -> p.getPlayerName().equals("한스"))
                    .findFirst()
                    .orElseThrow();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(한스Entity.getPlayerName()).isEqualTo("한스");
                softly.assertThat(한스Entity.getPlayerType()).isEqualTo(PlayerType.HOST);
                softly.assertThat(한스Entity.getRoomSession().getId()).isEqualTo(room.getId());
                softly.assertThat(한스Entity.getCreatedAt()).isNotNull();
            });
        }

        @Test
        void 호스트와_게스트의_타입이_올바르게_구분되어_저장된다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            final List<Player> players = List.of(
                    PlayerFixture.호스트한스(),
                    PlayerFixture.게스트꾹이()
            );

            // when
            playerSavePort.saveAll("ABCD", players);

            // then
            final List<PlayerEntity> saved = playerJpaRepository.findAllByRoomSession(room);
            final long hostCount = saved.stream()
                    .filter(p -> p.getPlayerType() == PlayerType.HOST)
                    .count();
            final long guestCount = saved.stream()
                    .filter(p -> p.getPlayerType() == PlayerType.GUEST)
                    .count();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(hostCount).isEqualTo(1);
                softly.assertThat(guestCount).isEqualTo(1);
            });
        }
    }
}
