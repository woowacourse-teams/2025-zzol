package coffeeshout.room.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.MenuCommandService;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PlayerServiceTest extends ServiceTest {

    @Autowired
    RoomService roomService;

    @Autowired
    RoomCommandService roomCommandService;

    @Autowired
    MenuCommandService menuCommandService;

    @Autowired
    PlayerService playerService;

    private void joinGuest(JoinCode joinCode, String guestName, SelectedMenuRequest selectedMenuRequest) {
        Menu menu = menuCommandService.convertMenu(selectedMenuRequest.id(), selectedMenuRequest.customName());
        roomCommandService.joinGuest(
                joinCode,
                new PlayerName(guestName),
                menu,
                selectedMenuRequest.temperature()
        );
    }

    @Test
    void 플레이어를_제거할_때_플레이어가_없다면_방을_제거한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, selectedMenuRequest);
        JoinCode joinCode = createdRoom.getJoinCode();

        // when
        playerService.removePlayer(joinCode.getValue(), hostName);

        // then
        assertThat(roomService.roomExists(joinCode.getValue())).isFalse();
    }

    @Test
    void 플레이어를_제거할_때_플레이어가_있다면_방을_제거하지_않는다() {
        String hostName = "호스트";
        SelectedMenuRequest hostSelectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, hostSelectedMenuRequest);
        JoinCode joinCode = createdRoom.getJoinCode();

        joinGuest(joinCode, "게스트1", new SelectedMenuRequest(2L, null, MenuTemperature.ICE));

        // when
        playerService.removePlayer(joinCode.getValue(), hostName);

        // then
        assertThat(roomService.roomExists(joinCode.getValue())).isTrue();
    }

    @Test
    void 방에_있는_모든_플레이어를_조회한다() {
        // given
        String hostName = "호스트";
        String guestName = "게스트";
        SelectedMenuRequest hostSelectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        SelectedMenuRequest guestSelectedMenuRequest = new SelectedMenuRequest(2L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, hostSelectedMenuRequest);
        joinGuest(createdRoom.getJoinCode(), guestName, guestSelectedMenuRequest);

        // when
        List<Player> players = playerService.getPlayers(createdRoom.getJoinCode().getValue());

        // then
        assertThat(players).hasSize(2);
        assertThat(players.stream().map(p -> p.getName().value()))
                .containsExactlyInAnyOrder(hostName, guestName);
    }
}
