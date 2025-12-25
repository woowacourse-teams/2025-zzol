package coffeeshout.room.application;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import coffeeshout.fixture.MenuFixture;
import coffeeshout.fixture.MiniGameDummy;
import coffeeshout.fixture.PlayerFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.global.exception.custom.NotExistElementException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.service.DelayedRoomRemovalService;
import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.QrCodeStatus;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.menu.SelectedMenu;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.service.MenuCommandService;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import coffeeshout.room.ui.response.ProbabilityResponse;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

class RoomServiceTest extends ServiceTest {

    @Autowired
    RoomService roomService;

    @MockitoSpyBean
    DelayedRoomRemovalService delayedRoomRemovalService;

    @Autowired
    RoomQueryService roomQueryService;

    @Autowired
    RoomCommandService roomCommandService;

    @Autowired
    MenuCommandService menuCommandService;

    // 테스트 헬퍼 메서드: enterRoom 대체
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
    void 방을_생성한다() {
        // given
        String hostName = "호스트짱";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);

        // when
        Room room = roomService.createRoom(hostName, selectedMenuRequest);

        // then
        assertThat(room).isNotNull();
        assertThat(room.getJoinCode()).isNotNull();
        assertThat(room.getRoomState()).isEqualTo(RoomState.READY);

        assertThat(room.getPlayers()).hasSize(1);
        assertThat(room.getPlayers().getFirst().getName().value()).isEqualTo(hostName);
        assertThat(room.isHost(room.getPlayers().getFirst())).isTrue();
    }

    @Test
    void 존재하지_않는_메뉴로_방을_생성하면_예외를_반환한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(999L, null, MenuTemperature.ICE);

        // when & then
        assertThatThrownBy(() -> roomService.createRoom(hostName, selectedMenuRequest))
                .isInstanceOf(NotExistElementException.class);
    }

    @Test
    void 플레이어가_메뉴를_선택한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest initialSelectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, initialSelectedMenuRequest);

        // when
        List<Player> players = roomService.selectMenu(createdRoom.getJoinCode().getValue(), hostName, 1L);
        Player host = players.getFirst();

        // then
        assertThat(host.getSelectedMenu().menu().getId()).isEqualTo(1L);
    }

    @Test
    void 존재하지_않는_플레이어가_메뉴를_선택하면_예외가_발생한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest initialSelectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, initialSelectedMenuRequest);
        String invalidPlayerName = "없는사람";

        // when & then
        assertThatThrownBy(
                () -> roomService.selectMenu(createdRoom.getJoinCode().getValue(), invalidPlayerName,
                        3L)).isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void 플레이어들의_확률을_조회한다() {
        // given
        String hostName = "호스트";
        String guestName = "게스트";
        SelectedMenuRequest hostSelectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        SelectedMenuRequest guestSelectedMenuRequest = new SelectedMenuRequest(2L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, hostSelectedMenuRequest);
        joinGuest(createdRoom.getJoinCode(), guestName, guestSelectedMenuRequest);

        // when
        List<ProbabilityResponse> probabilities = roomService.getProbabilities(createdRoom.getJoinCode().getValue());

        // then
        assertThat(probabilities).hasSize(2);
        double totalProbability = probabilities.stream()
                .mapToDouble(ProbabilityResponse::probability)
                .sum();
        assertThat(totalProbability).isEqualTo(100.0);
    }

    @Test
    void 모든_미니게임_목록을_조회한다() {
        // when
        List<MiniGameType> miniGames = roomService.getAllMiniGames();

        // then
        assertThat(miniGames).containsExactlyInAnyOrder(MiniGameType.values());
    }

    @Test
    void 방이_존재하는지_확인한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, selectedMenuRequest);
        JoinCode joinCode = createdRoom.getJoinCode();

        // when & then
        assertThat(roomService.roomExists(joinCode.getValue())).isTrue();
        assertThat(roomService.roomExists("TRAS")).isFalse();
    }

    @Test
    void 중복된_이름의_플레이어가_존재하는지_확인한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, selectedMenuRequest);
        JoinCode joinCode = createdRoom.getJoinCode();

        PlayerName guestName = new PlayerName("게스트1");
        createdRoom.joinGuest(guestName, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));

        // when & then
        assertThat(roomService.isGuestNameDuplicated(joinCode.getValue(), guestName.value())).isTrue();
        assertThat(roomService.isGuestNameDuplicated(joinCode.getValue(), "uniqueName")).isFalse();
    }

    @Test
    void 룰렛을_돌려서_당첨자를_선택한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest hostSelectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, hostSelectedMenuRequest);
        joinGuest(createdRoom.getJoinCode(), "게스트1", new SelectedMenuRequest(2L, null, MenuTemperature.ICE));
        joinGuest(createdRoom.getJoinCode(), "게스트2", new SelectedMenuRequest(3L, null, MenuTemperature.ICE));
        ReflectionTestUtils.setField(createdRoom, "roomState", RoomState.ROULETTE);

        // when
        Winner winner = roomService.spinRoulette(createdRoom.getJoinCode().getValue(), hostName);

        // then
        assertThat(winner).isNotNull();
        assertThat(createdRoom.getPlayers().stream().map(Player::getName)).contains(winner.name());
    }

    @Test
    void 미니게임의_점수를_반환한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest hostSelectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, hostSelectedMenuRequest);
        JoinCode joinCode = createdRoom.getJoinCode();
        joinGuest(joinCode, "게스트1", new SelectedMenuRequest(2L, null, MenuTemperature.ICE));
        joinGuest(joinCode, "게스트2", new SelectedMenuRequest(3L, null, MenuTemperature.ICE));

        List<MiniGameDummy> miniGames = List.of(new MiniGameDummy());
        ReflectionTestUtils.setField(createdRoom, "finishedGames", miniGames);

        // when
        Map<Player, MiniGameScore> miniGameScores = roomService.getMiniGameScores(
                joinCode.getValue(),
                MiniGameType.CARD_GAME
        );

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(miniGameScores.get(PlayerFixture.호스트꾹이()).getValue()).isEqualTo(20);
            softly.assertThat(miniGameScores.get(PlayerFixture.게스트루키()).getValue()).isEqualTo(-10);
        });
    }

    @Test
    void 미니게임의_순위를_반환한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest hostSelectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, hostSelectedMenuRequest);
        JoinCode joinCode = createdRoom.getJoinCode();
        joinGuest(joinCode, "게스트1", new SelectedMenuRequest(2L, null, MenuTemperature.ICE));
        joinGuest(joinCode, "게스트2", new SelectedMenuRequest(3L, null, MenuTemperature.ICE));

        List<MiniGameDummy> miniGames = List.of(new MiniGameDummy());
        ReflectionTestUtils.setField(createdRoom, "finishedGames", miniGames);

        // when
        MiniGameResult miniGameRanks = roomService.getMiniGameRanks(joinCode.getValue(), MiniGameType.CARD_GAME);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(miniGameRanks.getPlayerRank(PlayerFixture.호스트꾹이())).isEqualTo(1);
            softly.assertThat(miniGameRanks.getPlayerRank(PlayerFixture.게스트루키())).isEqualTo(2);
        });
    }

    @Test
    void 선택된_미니게임의_목록을_반환한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest hostSelectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, hostSelectedMenuRequest);
        JoinCode joinCode = createdRoom.getJoinCode();
        joinGuest(joinCode, "게스트1", new SelectedMenuRequest(2L, null, MenuTemperature.ICE));
        joinGuest(joinCode, "게스트2", new SelectedMenuRequest(3L, null, MenuTemperature.ICE));
        roomCommandService.updateMiniGames(createdRoom.getJoinCode(), new PlayerName(hostName),
                List.of(MiniGameType.CARD_GAME));

        // when
        List<MiniGameType> selectedMiniGames = roomService.getSelectedMiniGames(joinCode.getValue());

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(selectedMiniGames).hasSize(1);
            softly.assertThat(selectedMiniGames).containsExactly(MiniGameType.CARD_GAME);
        });
    }

    @Test
    void 방_생성_시_QR_코드가_비동기로_생성된다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);

        // when
        Room createdRoom = roomService.createRoom(hostName, selectedMenuRequest);
        JoinCode joinCode = createdRoom.getJoinCode();

        // then

        // 비동기 작업이 완료될 때까지 대기 (최대 3초)
        await().atMost(3, SECONDS)
                .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Room room = roomQueryService.getByJoinCode(joinCode);
                    QrCodeStatus status = room.getJoinCode().getQrCode().getStatus();

                    // SUCCESS 또는 ERROR 상태로 변경되었는지 확인
                    assertThat(status).isIn(QrCodeStatus.SUCCESS, QrCodeStatus.ERROR);
                });
    }

    @Test
    void QR코드_상태를_조회한다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.ICE);
        Room createdRoom = roomService.createRoom(hostName, selectedMenuRequest);
        String joinCode = createdRoom.getJoinCode().getValue();

        // when
        QrCodeStatusResponse qrCodeStatus = roomService.getQrCodeStatus(joinCode);

        // then
        assertThat(qrCodeStatus.status()).isIn(QrCodeStatus.PENDING, QrCodeStatus.SUCCESS, QrCodeStatus.ERROR);
    }

    @Test
    void 존재하지_않는_방의_QR코드_상태를_조회하면_예외를_반환한다() {
        // given
        String nonExistentJoinCode = "NXNX";

        // when & then
        assertThatThrownBy(() -> roomService.getQrCodeStatus(nonExistentJoinCode))
                .isInstanceOf(NotExistElementException.class);
    }
}
