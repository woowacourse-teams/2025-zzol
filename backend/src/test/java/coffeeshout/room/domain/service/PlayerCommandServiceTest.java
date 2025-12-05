package coffeeshout.room.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.ServiceTest;
import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PlayerCommandServiceTest extends ServiceTest {

    @Autowired
    RoomService roomService;

    @Autowired
    PlayerCommandService playerCommandService;

    @Test
    void 방에_있는_모든_플레이어를_조회한다() {
        // given
        String hostName = "호스트";
        String guestName = "게스트";
        Room createdRoom = roomService.createRoom(hostName);
        roomService.enterRoom(createdRoom.getJoinCode().getValue(), guestName);

        // when
        List<Player> players = playerCommandService.getAllPlayers(createdRoom.getJoinCode().getValue());

        // then
        assertThat(players).hasSize(2);
        assertThat(players.stream().map(p -> p.getName().value()))
                .containsExactlyInAnyOrder(hostName, guestName);
    }

    @Test
    void 중복된_이름의_플레이어가_존재하는지_확인한다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName);
        JoinCode joinCode = createdRoom.getJoinCode();

        PlayerName guestName = new PlayerName("게스트1");
        createdRoom.joinGuest(guestName);

        // when & then
        assertThat(playerCommandService.isGuestNameDuplicated(joinCode.getValue(), guestName.value())).isTrue();
        assertThat(playerCommandService.isGuestNameDuplicated(joinCode.getValue(), "uniqueName")).isFalse();
    }

    @Test
    void 게스트가_준비_상태를_변경한다() {
        // given
        String hostName = "호스트";
        String guestName = "게스트";
        Room createdRoom = roomService.createRoom(hostName);
        roomService.enterRoom(createdRoom.getJoinCode().getValue(), guestName);

        // when
        List<Player> players = playerCommandService.changePlayerReadyState(
                createdRoom.getJoinCode().getValue(),
                guestName,
                true
        );

        // then
        Player guest = players.stream()
                .filter(p -> p.getName().value().equals(guestName))
                .findFirst()
                .orElseThrow();
        assertThat(guest.getIsReady()).isTrue();
    }

    @Test
    void 호스트가_준비_상태를_변경해도_변경되지_않는다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName);

        // when
        List<Player> players = playerCommandService.changePlayerReadyState(
                createdRoom.getJoinCode().getValue(),
                hostName,
                true
        );

        // then
        Player host = players.stream()
                .filter(p -> p.getName().value().equals(hostName))
                .findFirst()
                .orElseThrow();
        // 호스트는 항상 준비 상태이므로 변경되지 않음
        assertThat(host.getIsReady()).isTrue();
    }

    @Test
    void 플레이어를_제거할_때_플레이어가_없다면_방을_제거한다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName);
        JoinCode joinCode = createdRoom.getJoinCode();

        // when
        playerCommandService.removePlayer(joinCode.getValue(), hostName);

        // then
        assertThat(roomService.roomExists(joinCode.getValue())).isFalse();
    }

    @Test
    void 플레이어를_제거할_때_플레이어가_있다면_방을_제거하지_않는다() {
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName);
        JoinCode joinCode = createdRoom.getJoinCode();
        roomService.enterRoom(createdRoom.getJoinCode().getValue(), "게스트1");

        // when
        playerCommandService.removePlayer(joinCode.getValue(), hostName);

        // then
        assertThat(roomService.roomExists(joinCode.getValue())).isTrue();
    }
}
