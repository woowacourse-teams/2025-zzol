package coffeeshout.room.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.MenuFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.player.PlayerName;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoomCommandServiceTest extends ServiceTest {

    @Autowired
    JoinCodeGenerator joinCodeGenerator;

    @Autowired
    RoomCommandService roomCommandService;

    @Autowired
    RoomQueryService roomQueryService;

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
            Room room = roomCommandService.saveIfAbsentRoom(joinCode, hostName, MenuFixture.아메리카노(), MenuTemperature.HOT);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(room.getJoinCode()).isEqualTo(joinCode);
                softly.assertThat(room.getPlayers()).hasSize(1);
                softly.assertThat(room.getPlayers().get(0).getName()).isEqualTo(hostName);
                softly.assertThat(room.getPlayers().get(0).getSelectedMenu().menu().getName()).isEqualTo("아메리카노");
                softly.assertThat(room.getPlayers().get(0).getSelectedMenu().menuTemperature())
                        .isEqualTo(MenuTemperature.HOT);
            });
        }

        @Test
        void 이미_존재하는_조인코드로_방을_생성하면_저장하지_않는다() {
            // given
            PlayerName hostName1 = new PlayerName("호스트1");
            PlayerName hostName2 = new PlayerName("호스트2");

            // when
            roomCommandService.saveIfAbsentRoom(joinCode, hostName1, MenuFixture.아메리카노(), MenuTemperature.HOT);
            roomCommandService.saveIfAbsentRoom(joinCode, hostName2, MenuFixture.라떼(), MenuTemperature.ICE);

            // then
            Room room = roomQueryService.getByJoinCode(joinCode);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(room.getPlayers()).hasSize(1);
                softly.assertThat(room.getPlayers().get(0).getName()).isEqualTo(hostName1);
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

            roomCommandService.saveIfAbsentRoom(joinCode, hostName, MenuFixture.아메리카노(), MenuTemperature.HOT);

            // when
            Room room = roomCommandService.joinGuest(joinCode, guestName, MenuFixture.라떼(), MenuTemperature.ICE);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(room.getPlayers()).hasSize(2);
                softly.assertThat(room.getPlayers().get(1).getName()).isEqualTo(guestName);
                softly.assertThat(room.getPlayers().get(1).getSelectedMenu().menu().getName()).isEqualTo("라떼");
                softly.assertThat(room.getPlayers().get(1).getSelectedMenu().menuTemperature())
                        .isEqualTo(MenuTemperature.ICE);
            });
        }

        @Test
        void 여러_게스트가_순차적으로_방에_입장한다() {
            // given
            PlayerName hostName = new PlayerName("호스트");
            PlayerName guest1 = new PlayerName("게스트1");
            PlayerName guest2 = new PlayerName("게스트2");
            PlayerName guest3 = new PlayerName("게스트3");

            roomCommandService.saveIfAbsentRoom(joinCode, hostName, MenuFixture.아메리카노(), MenuTemperature.HOT);

            // when
            roomCommandService.joinGuest(joinCode, guest1, MenuFixture.라떼(), MenuTemperature.ICE);
            roomCommandService.joinGuest(joinCode, guest2, MenuFixture.아이스티(), MenuTemperature.ICE);
            Room room = roomCommandService.joinGuest(joinCode, guest3, MenuFixture.아메리카노(), MenuTemperature.ICE);

            // then
            assertThat(room.getPlayers()).hasSize(4);
            assertThat(room.getPlayers())
                    .extracting(player -> player.getName().value())
                    .containsExactly("호스트", "게스트1", "게스트2", "게스트3");
        }
    }
}
