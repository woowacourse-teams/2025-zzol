package coffeeshout.room.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoomUserIdTest {

    @Nested
    class 방_생성 {

        @Test
        void 회원_호스트_userId가_플레이어에_저장된다() {
            final Room room = Room.createNewRoom(new JoinCode("A4BX"), new PlayerName("꾹이"), 7L, 0.7);

            final Player host = room.findPlayer(new PlayerName("꾹이"));

            assertThat(host.getUserId()).isEqualTo(7L);
        }

        @Test
        void 익명_호스트_userId는_null이다() {
            final Room room = Room.createNewRoom(new JoinCode("B4CX"), new PlayerName("꾹이"), null, 0.7);

            final Player host = room.findPlayer(new PlayerName("꾹이"));

            assertThat(host.getUserId()).isNull();
        }
    }

    @Nested
    class 게스트_입장 {

        @Test
        void 회원_게스트_userId가_플레이어에_저장된다() {
            final Room room = Room.createNewRoom(new JoinCode("C4DX"), new PlayerName("꾹이"), null, 0.7);

            room.joinGuest(new PlayerName("루키"), 55L);

            final Player guest = room.findPlayer(new PlayerName("루키"));
            assertThat(guest.getUserId()).isEqualTo(55L);
        }

        @Test
        void 익명_게스트_userId는_null이다() {
            final Room room = Room.createNewRoom(new JoinCode("D4FX"), new PlayerName("꾹이"), null, 0.7);

            room.joinGuest(new PlayerName("루키"));

            final Player guest = room.findPlayer(new PlayerName("루키"));
            assertThat(guest.getUserId()).isNull();
        }
    }
}
