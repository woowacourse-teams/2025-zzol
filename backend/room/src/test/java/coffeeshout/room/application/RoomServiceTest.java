package coffeeshout.room.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

import coffeeshout.RoomModuleServiceTest;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.profanity.application.ProfanityFilterService;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.fixture.ProfanityTestDataSeeder;
import coffeeshout.room.application.service.DelayedRoomRemovalService;
import coffeeshout.room.application.service.RoomCommandService;
import coffeeshout.room.application.service.RoomCreateResult;
import coffeeshout.room.application.service.RoomEnterResult;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.application.service.RoulettePersistenceService;
import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.QrCodeStatus;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.infra.auth.RoomSessionClaim;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.room.infra.messaging.RoomEventWaitManager;
import coffeeshout.room.ui.response.ProbabilityResponse;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

class RoomServiceTest extends RoomModuleServiceTest {

    @Autowired
    RoomService roomService;

    @MockitoSpyBean
    DelayedRoomRemovalService delayedRoomRemovalService;

    // 룰렛 결과 DB 저장은 이 테스트의 관심사가 아니고, PlayerEntity 영속을 요구해 픽스처만 무거워진다.
    @MockitoBean
    RoulettePersistenceService roulettePersistenceService;

    @MockitoSpyBean
    RoomEventWaitManager roomEventWaitManager;

    @Autowired
    RoomQueryService roomQueryService;

    @Autowired
    RoomCommandService roomCommandService;

    @Autowired
    RoomSessionTokenService roomSessionTokenService;

    @Autowired
    ProfanityWordRepository profanityWordRepository;

    @Autowired
    ProfanityFilterService profanityFilterService;

    // 테스트 헬퍼 메서드: enterRoom 대체
    private void joinGuest(JoinCode joinCode, String guestName) {
        roomCommandService.joinGuest(joinCode, new PlayerName(guestName));
    }

    @Nested
    class 방_생성_시_RST {

        @Test
        void 게스트_방_생성_RST에는_올바른_클레임이_담겨있다() {
            final RoomCreateResult result = roomService.createRoom("호스트");
            final RoomSessionClaim claim = roomSessionTokenService.verify(result.roomSessionToken());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(claim.joinCode()).isEqualTo(result.room().getJoinCode().getValue());
                softly.assertThat(claim.playerName()).isEqualTo("호스트");
                softly.assertThat(claim.userId()).isNull();
            });
        }
    }

    @Nested
    class 방_입장_시_RST {

        @Test
        void 게스트_방_입장_RST에는_올바른_클레임이_담겨있다() throws Exception {
            final Room room = roomService.createRoom("호스트").room();
            final String joinCode = room.getJoinCode().getValue();
            doReturn(CompletableFuture.completedFuture(room)).when(roomEventWaitManager).registerWait(anyString());

            final RoomEnterResult result = roomService.enterRoomAsync(joinCode, "게스트").get();
            final RoomSessionClaim claim = roomSessionTokenService.verify(result.roomSessionToken());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(claim.joinCode()).isEqualTo(joinCode);
                softly.assertThat(claim.playerName()).isEqualTo("게스트");
                softly.assertThat(claim.userId()).isNull();
            });
        }
    }

    @Nested
    class 랜덤_닉네임_생성 {

        @Test
        void joinCode_없이_랜덤_닉네임을_생성한다() {
            final String nickname = roomService.generateRandomNicknameForHost();

            assertThat(nickname).isNotBlank();
            assertThat(nickname.length()).isLessThanOrEqualTo(10);
        }

        @Test
        void joinCode_있으면_기존_플레이어와_중복되지_않는_닉네임을_생성한다() {
            Room createdRoom = roomService.createRoom("호스트").room();
            joinGuest(createdRoom.getJoinCode(), "게스트1");
            joinGuest(createdRoom.getJoinCode(), "게스트2");

            final String nickname = roomService.generateRandomNicknameForGuest(createdRoom.getJoinCode().getValue());

            assertThat(nickname).isNotIn("호스트", "게스트1", "게스트2");
        }
    }

    @Nested
    class 닉네임_비속어_검증 {

        @BeforeEach
        void seedProfanityWords() {
            new ProfanityTestDataSeeder(profanityWordRepository, profanityFilterService).seedForTest();
        }

        @Test
        void 정상_닉네임으로_방을_생성한다() {
            assertThatCode(() -> roomService.createRoom("용감한호랑이"))
                    .doesNotThrowAnyException();
        }

        @Test
        void 비속어_호스트_닉네임으로_방_생성이_실패한다() {
            assertCoffeeShoutException(
                    () -> roomService.createRoom("씨발"),
                    RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY
            );
        }

        @Test
        void 공백_우회_비속어_호스트_닉네임으로_방_생성이_실패한다() {
            assertCoffeeShoutException(
                    () -> roomService.createRoom("씨 발"),
                    RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY
            );
        }

        @Test
        void 비속어_게스트_닉네임으로_방_입장이_실패한다() {
            Room createdRoom = roomService.createRoom("호스트").room();

            String joinCode = createdRoom.getJoinCode().getValue();
            assertCoffeeShoutException(
                    () -> roomService.enterRoomAsync(joinCode, "씨발"),
                    RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY
            );
        }
    }

    @Test
    void 방을_생성한다() {
        // given
        String hostName = "호스트짱";

        // when
        Room room = roomService.createRoom(hostName).room();

        // then
        assertThat(room).isNotNull();
        assertThat(room.getJoinCode()).isNotNull();
        assertThat(room.getRoomState()).isEqualTo(RoomState.READY);

        assertThat(room.getPlayers()).hasSize(1);
        assertThat(room.getPlayers().getFirst().getName().value()).isEqualTo(hostName);
        assertThat(room.isHost(room.getPlayers().getFirst())).isTrue();
    }

    @Test
    void 플레이어들의_확률을_조회한다() {
        // given
        String hostName = "호스트";
        String guestName = "게스트";
        Room createdRoom = roomService.createRoom(hostName).room();
        joinGuest(createdRoom.getJoinCode(), guestName);

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
    void 방이_존재하는지_확인한다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName).room();
        JoinCode joinCode = createdRoom.getJoinCode();

        // when & then
        assertThat(roomService.roomExists(joinCode.getValue())).isTrue();
        assertThat(roomService.roomExists("TRAS")).isFalse();
    }

    @Test
    void 중복된_이름의_플레이어가_존재하는지_확인한다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName).room();
        JoinCode joinCode = createdRoom.getJoinCode();

        PlayerName guestName = new PlayerName("게스트1");
        createdRoom.joinGuest(guestName);

        // when & then
        assertThat(roomService.isGuestNameDuplicated(joinCode.getValue(), guestName.value())).isTrue();
        assertThat(roomService.isGuestNameDuplicated(joinCode.getValue(), "uniqueName")).isFalse();
    }

    @Test
    void 룰렛을_돌려서_당첨자를_선택한다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName).room();
        joinGuest(createdRoom.getJoinCode(), "게스트1");
        joinGuest(createdRoom.getJoinCode(), "게스트2");
        ReflectionTestUtils.setField(createdRoom, "roomState", RoomState.ROULETTE);

        // when
        Winner winner = roomService.spinRoulette(createdRoom.getJoinCode().getValue(), hostName);

        // then
        assertThat(winner).isNotNull();
        assertThat(createdRoom.getPlayers().stream().map(Player::getName)).contains(winner.name());
    }

    @Test
    void 룰렛_결과_처리_후_끝난_방_정리를_예약한다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName).room();
        JoinCode joinCode = createdRoom.getJoinCode();
        joinGuest(joinCode, "게스트1");
        ReflectionTestUtils.setField(createdRoom, "roomState", RoomState.ROULETTE);
        Winner winner = roomService.spinRoulette(joinCode.getValue(), hostName);

        // when
        roomService.spinRoulette(new RouletteSpinEvent(joinCode.getValue(), hostName, winner));

        // then — 생성 시 걸어 둔 최대 수명이 아니라 끝난 방 전용 지연으로 예약돼야 한다
        then(delayedRoomRemovalService).should().scheduleRemoveFinishedRoom(joinCode);
    }

    @Test
    void 방_생성_시_QR_코드가_비동기로_생성된다() {
        // given
        String hostName = "호스트";

        // when
        Room createdRoom = roomService.createRoom(hostName).room();
        JoinCode joinCode = createdRoom.getJoinCode();

        // then

        // 비동기 작업이 완료될 때까지 대기 (최대 3초)
        await().atMost(3, SECONDS)
                .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Room room = roomQueryService.getByJoinCode(joinCode);
                    QrCodeStatus status = room.getQrCode().getStatus();

                    // SUCCESS 또는 ERROR 상태로 변경되었는지 확인
                    assertThat(status).isIn(QrCodeStatus.SUCCESS, QrCodeStatus.ERROR);
                });
    }

    @Test
    void QR코드_상태를_조회한다() {
        // given
        String hostName = "호스트";
        Room createdRoom = roomService.createRoom(hostName).room();
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
        assertCoffeeShoutException(
                () -> roomService.getQrCodeStatus(nonExistentJoinCode),
                GlobalErrorCode.NOT_EXIST
        );
    }
}
