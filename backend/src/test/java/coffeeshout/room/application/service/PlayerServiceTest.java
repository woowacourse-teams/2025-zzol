package coffeeshout.room.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.ServiceTest;
import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.global.exception.custom.NotExistElementException;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomCommandService;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PlayerServiceTest extends ServiceTest {

    @Autowired
    RoomService roomService;

    @Autowired
    RoomCommandService roomCommandService;

    @Autowired
    PlayerService playerService;

    @MockitoBean
    StreamPublisher streamPublisher;

    private void joinGuest(JoinCode joinCode, String guestName) {
        roomCommandService.joinGuest(joinCode, new PlayerName(guestName));
    }

    @Nested
    class 플레이어_제거 {

        @Test
        void 호스트를_제거할_때_플레이어가_없다면_방을_제거한다() {
            // given
            String hostName = "호스트";
            Room createdRoom = roomService.createRoom(hostName);
            JoinCode joinCode = createdRoom.getJoinCode();

            // when
            playerService.kickPlayer(joinCode.getValue(), hostName);

            // then
            assertThat(roomService.roomExists(joinCode.getValue())).isFalse();
        }

        @Test
        void 호스트를_제거할_때_게스트가_있다면_방을_제거하지_않는다() {
            // given
            String hostName = "호스트";
            Room createdRoom = roomService.createRoom(hostName);
            JoinCode joinCode = createdRoom.getJoinCode();

            joinGuest(joinCode, "게스트1");

            // when
            playerService.kickPlayer(joinCode.getValue(), hostName);

            // then
            assertThat(roomService.roomExists(joinCode.getValue())).isTrue();
        }

        @Nested
        class 게스트_제거 {

            @Test
            void 게스트를_제거했을_때_방이_유지되고_플레이어_목록에서_제외된다() {
                // given
                String hostName = "호스트";
                String guestName = "게스트";
                Room createdRoom = roomService.createRoom(hostName);
                JoinCode joinCode = createdRoom.getJoinCode();
                joinGuest(joinCode, guestName);

                // when
                playerService.kickPlayer(joinCode.getValue(), guestName);

                // then
                assertThat(roomService.roomExists(joinCode.getValue())).isTrue();
                List<Player> players = playerService.getPlayers(joinCode.getValue());
                assertThat(players).hasSize(1);
                assertThat(players.getFirst().getName().value()).isEqualTo(hostName);
            }

            @Test
            void 존재하지_않는_게스트_제거_시도_시_예외가_발생한다() {
                // given
                String hostName = "호스트";
                Room createdRoom = roomService.createRoom(hostName);
                JoinCode joinCode = createdRoom.getJoinCode();

                // when & then
                assertThatThrownBy(() -> playerService.kickPlayer(joinCode.getValue(), "존재하지_않는_게스트"))
                        .isInstanceOf(InvalidArgumentException.class);
            }

            @Test
            void null_플레이어_이름으로_제거_시도_시_예외가_발생한다() {
                // given
                String hostName = "호스트";
                Room createdRoom = roomService.createRoom(hostName);
                JoinCode joinCode = createdRoom.getJoinCode();

                // when & then
                assertThatThrownBy(() -> playerService.kickPlayer(joinCode.getValue(), null))
                        .isInstanceOf(InvalidArgumentException.class);
            }
        }

        @Test
        void 존재하지_않는_방_코드로_제거_시도_시_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> playerService.kickPlayer("ABCD", "플레이어"))
                    .isInstanceOf(NotExistElementException.class);
        }

        @Test
        void null_방_코드로_제거_시도_시_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> playerService.kickPlayer(null, "플레이어"))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class 플레이어_조회 {

        @Test
        void 방에_있는_모든_플레이어를_조회한다() {
            // given
            String hostName = "호스트";
            String guestName = "게스트";
            Room createdRoom = roomService.createRoom(hostName);
            joinGuest(createdRoom.getJoinCode(), guestName);

            // when
            List<Player> players = playerService.getPlayers(createdRoom.getJoinCode().getValue());

            // then
            assertThat(players).hasSize(2);
            assertThat(players.stream().map(p -> p.getName().value()))
                    .containsExactlyInAnyOrder(hostName, guestName);
        }

        @Test
        void 빈_방에서_플레이어_조회_시_예외가_발생한다() {
            // given
            String hostName = "호스트";
            Room createdRoom = roomService.createRoom(hostName);
            JoinCode joinCode = createdRoom.getJoinCode();

            // 모든 플레이어 제거 (방도 제거됨)
            playerService.kickPlayer(joinCode.getValue(), hostName);

            // when & then
            assertThatThrownBy(() -> playerService.getPlayers(joinCode.getValue()))
                    .isInstanceOf(NotExistElementException.class);
        }

        @Test
        void 존재하지_않는_방_코드로_조회_시_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> playerService.getPlayers("ABCD"))
                    .isInstanceOf(NotExistElementException.class);
        }

        @Test
        void null_방_코드로_조회_시_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> playerService.getPlayers(null))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }
}
