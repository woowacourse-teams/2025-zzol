package coffeeshout.room.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.ui.response.ProbabilityResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class RouletteCommandServiceTest extends ServiceTest {

    @Autowired
    RoomService roomService;

    @Autowired
    RouletteCommandService rouletteCommandService;

    @Test
    void 플레이어들의_확률을_조회한다() {
        // given
        String hostName = "호스트";
        String guestName = "게스트";
        Room createdRoom = roomService.createRoom(hostName);
        roomService.enterRoom(createdRoom.getJoinCode().getValue(), guestName);

        // when
        List<ProbabilityResponse> probabilities = rouletteCommandService.getProbabilities(
                createdRoom.getJoinCode().getValue()
        );

        // then
        assertThat(probabilities).hasSize(2);
        double totalProbability = probabilities.stream()
                .mapToDouble(ProbabilityResponse::probability)
                .sum();
        assertThat(totalProbability).isEqualTo(100.0);
    }

    @Test
    void 룰렛을_돌려서_당첨자를_선택한다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName);
        roomService.enterRoom(createdRoom.getJoinCode().getValue(), "게스트1");
        roomService.enterRoom(createdRoom.getJoinCode().getValue(), "게스트2");
        ReflectionTestUtils.setField(createdRoom, "roomState", RoomState.ROULETTE);

        // when
        Winner winner = rouletteCommandService.spinRoulette(createdRoom.getJoinCode().getValue(), hostName);

        // then
        assertThat(winner).isNotNull();
        assertThat(createdRoom.getPlayers().stream().map(Player::getName)).contains(winner.name());
    }

    @Test
    void 룰렛을_표시한다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName);
        roomService.enterRoom(createdRoom.getJoinCode().getValue(), "게스트1");

        // when
        Room room = rouletteCommandService.showRoulette(createdRoom.getJoinCode().getValue());

        // then
        assertThat(room.getRoomState()).isEqualTo(RoomState.ROULETTE);
    }
}
