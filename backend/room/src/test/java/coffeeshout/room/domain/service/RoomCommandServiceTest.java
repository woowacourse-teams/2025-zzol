package coffeeshout.room.domain.service;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.RoomModuleServiceTest;
import coffeeshout.fixture.TestDataHelper;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.room.application.service.RoomCommandService;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoomCommandServiceTest extends RoomModuleServiceTest {

    @Autowired
    JoinCodeGenerator joinCodeGenerator;

    @Autowired
    RoomCommandService roomCommandService;

    @Autowired
    RoomQueryService roomQueryService;

    @Autowired
    TestDataHelper testDataHelper;

    JoinCode joinCode;

    @BeforeEach
    void setUp() {
        joinCode = joinCodeGenerator.generate();
    }

    @Nested
    class 방_생성 {

        @Test
        void 방을_생성한다() {
            // given
            PlayerName hostName = new PlayerName("호스트");

            // when
            Room room = roomCommandService.saveIfAbsentRoom(joinCode, hostName, 0.7);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(room.getJoinCode()).isEqualTo(joinCode);
                softly.assertThat(room.getPlayers()).hasSize(1);
                softly.assertThat(room.getPlayers().getFirst().getName()).isEqualTo(hostName);
            });
        }

        @Test
        void 이미_존재하는_조인코드로_방을_생성하면_저장하지_않는다() {
            // given
            PlayerName hostName1 = new PlayerName("호스트1");
            PlayerName hostName2 = new PlayerName("호스트2");

            // when
            roomCommandService.saveIfAbsentRoom(joinCode, hostName1, 0.7);
            roomCommandService.saveIfAbsentRoom(joinCode, hostName2, 0.7);

            // then
            Room room = roomQueryService.getByJoinCode(joinCode);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(room.getPlayers()).hasSize(1);
                softly.assertThat(room.getPlayers().getFirst().getName()).isEqualTo(hostName1);
            });
        }
    }

    @Nested
    class 게스트_입장 {

        @Test
        void 게스트가_방에_입장한다() {
            // given
            PlayerName hostName = new PlayerName("호스트");
            PlayerName guestName = new PlayerName("게스트");

            roomCommandService.saveIfAbsentRoom(joinCode, hostName, 0.7);

            // when
            Room room = roomCommandService.joinGuest(joinCode, guestName);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(room.getPlayers()).hasSize(2);
                softly.assertThat(room.getPlayers().get(1).getName()).isEqualTo(guestName);
            });
        }

        @Test
        void 여러_게스트가_순차적으로_방에_입장한다() {
            // given
            PlayerName hostName = new PlayerName("호스트");
            PlayerName guest1 = new PlayerName("게스트1");
            PlayerName guest2 = new PlayerName("게스트2");
            PlayerName guest3 = new PlayerName("게스트3");

            roomCommandService.saveIfAbsentRoom(joinCode, hostName, 0.7);

            // when
            roomCommandService.joinGuest(joinCode, guest1);
            roomCommandService.joinGuest(joinCode, guest2);
            Room room = roomCommandService.joinGuest(joinCode, guest3);

            // then
            assertThat(room.getPlayers()).hasSize(4);
            assertThat(room.getPlayers())
                    .extracting(player -> player.getName().value())
                    .containsExactly("호스트", "게스트1", "게스트2", "게스트3");
        }

        @Test
        void 존재하지_않는_조인코드로_입장하면_예외가_발생한다() {
            // given
            JoinCode invalidJoinCode = new JoinCode("ABCD");
            PlayerName guestName = new PlayerName("게스트");

            // when & then
            assertCoffeeShoutException(
                    () -> roomCommandService.joinGuest(invalidJoinCode, guestName),
                    GlobalErrorCode.NOT_EXIST
            );
        }

        @Test
        void 게임_중인_방에_입장할_수_없다() {
            // given
            JoinCode existingJoinCode = joinCodeGenerator.generate();
            PlayerName guestName = new PlayerName("더미게스트");

            testDataHelper.진행중인_방_생성(existingJoinCode, new PlayerName("더미호스트"));

            // when & then
            assertCoffeeShoutException(
                    () -> roomCommandService.joinGuest(existingJoinCode, guestName),
                    RoomErrorCode.ROOM_NOT_READY_TO_JOIN
            );
        }

        @Test
        void 동일한_조인코드로_여러_게스트가_입장_가능() {
            // given
            PlayerName hostName = new PlayerName("호스트짱");
            JoinCode testJoinCode = joinCodeGenerator.generate();

            roomCommandService.saveIfAbsentRoom(testJoinCode, hostName, 0.7);

            // when
            roomCommandService.joinGuest(testJoinCode, new PlayerName("게스트1"));
            roomCommandService.joinGuest(testJoinCode, new PlayerName("게스트2"));
            Room result = roomCommandService.joinGuest(testJoinCode, new PlayerName("게스트3"));

            // then
            assertThat(result.getPlayers()).hasSize(4);
            assertThat(result.getPlayers().stream().map(p -> p.getName().value()))
                    .contains("호스트짱", "게스트1", "게스트2", "게스트3");
        }

        @Test
        void 최대_인원에서_입장을_하면_예외를_반환한다() {
            // given
            PlayerName hostName = new PlayerName("호스트짱");
            JoinCode testJoinCode = joinCodeGenerator.generate();

            roomCommandService.saveIfAbsentRoom(testJoinCode, hostName, 0.7);

            // 최대 9명까지니까 8명 더 넣어보기
            for (int i = 2; i <= 9; i++) {
                roomCommandService.joinGuest(testJoinCode, new PlayerName("게스트" + i));
            }

            // when & then
            assertCoffeeShoutException(
                    () -> roomCommandService.joinGuest(testJoinCode, new PlayerName("게스트10")),
                    RoomErrorCode.ROOM_FULL
            );
        }

        @Test
        void 중복된_이름으로_입장할_수_없다() {
            // given
            PlayerName hostName = new PlayerName("호스트짱");
            JoinCode testJoinCode = joinCodeGenerator.generate();

            roomCommandService.saveIfAbsentRoom(testJoinCode, hostName, 0.7);
            roomCommandService.joinGuest(testJoinCode, new PlayerName("게스트"));

            // when & then
            assertCoffeeShoutException(
                    () -> roomCommandService.joinGuest(testJoinCode, new PlayerName("게스트")),
                    RoomErrorCode.DUPLICATE_PLAYER_NAME
            );
        }

    }

    @Nested
    class 게임_결과_적용 {

        @Test
        void 순위_맵과_라운드_수로_확률을_조정하고_SCORE_BOARD로_전이한다() {
            // given
            PlayerName hostName = new PlayerName("호스트");
            PlayerName guestName = new PlayerName("게스트");
            roomCommandService.saveIfAbsentRoom(joinCode, hostName, 0.7);
            roomCommandService.joinGuest(joinCode, guestName);

            Map<PlayerName, Integer> rankByPlayer = Map.of(hostName, 1, guestName, 2);

            // when
            roomCommandService.applyGameResult(joinCode, rankByPlayer, 5);

            // then
            Room room = roomQueryService.getByJoinCode(joinCode);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(room.getRoomState()).isEqualTo(RoomState.SCORE_BOARD);
                softly.assertThat(room.findPlayer(hostName).getProbability().value())
                        .isLessThan(room.findPlayer(guestName).getProbability().value());
            });
        }
    }
}
