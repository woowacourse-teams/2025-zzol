package coffeeshout.room.infra.messaging.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import coffeeshout.global.ServiceTest;
import coffeeshout.room.application.DelayedRoomRemovalService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class RoomCreateEventHandlerTest extends ServiceTest {

    @Autowired
    RoomCreateEventHandler roomCreateEventHandler;

    @Autowired
    RoomQueryService roomQueryService;

    @Autowired
    JoinCodeGenerator joinCodeGenerator;

    @MockitoSpyBean
    DelayedRoomRemovalService delayedRoomRemovalService;

    JoinCode joinCode;

    @BeforeEach
    void setUp() {
        joinCode = joinCodeGenerator.generate();
    }

    @Test
    void 방_생성_이벤트를_처리하면_방이_생성된다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.HOT);

        RoomCreateEvent event = new RoomCreateEvent(
                hostName,
                selectedMenuRequest,
                joinCode.getValue()
        );

        // when
        roomCreateEventHandler.handle(event);

        // then
        Room room = roomQueryService.getByJoinCode(joinCode);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(room.getJoinCode()).isEqualTo(joinCode);
            softly.assertThat(room.getPlayers()).hasSize(1);
            softly.assertThat(room.getPlayers().get(0).getName().value()).isEqualTo(hostName);
            softly.assertThat(room.getPlayers().get(0).getSelectedMenu().menuTemperature())
                    .isEqualTo(MenuTemperature.HOT);
        });
    }

    @Test
    void 방_생성_이벤트_처리_시_방_삭제_스케줄러가_호출된다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.HOT);

        RoomCreateEvent event = new RoomCreateEvent(
                hostName,
                selectedMenuRequest,
                joinCode.getValue()
        );

        // when
        roomCreateEventHandler.handle(event);

        // then
        verify(delayedRoomRemovalService).scheduleRemoveRoom(joinCode);
    }

    @Test
    void 커스텀_메뉴로_방_생성_이벤트를_처리할_수_있다() {
        // given
        String hostName = "호스트";
        String customMenuName = "커스텀아메리카노";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(0L, customMenuName, MenuTemperature.ICE);

        RoomCreateEvent event = new RoomCreateEvent(
                hostName,
                selectedMenuRequest,
                joinCode.getValue()
        );

        // when
        roomCreateEventHandler.handle(event);

        // then
        Room room = roomQueryService.getByJoinCode(joinCode);
        assertThat(room.getPlayers().get(0).getSelectedMenu().menu().getName()).isEqualTo(customMenuName);
    }

    @Test
    void 동일_joinCode로_이벤트가_중복_발행되어도_방은_한_번만_생성된다() {
        // given
        String hostName = "호스트";
        SelectedMenuRequest selectedMenuRequest = new SelectedMenuRequest(1L, null, MenuTemperature.HOT);
        String qrCodeUrl = "https://example.com/qr";

        RoomCreateEvent event = new RoomCreateEvent(
                hostName,
                selectedMenuRequest,
                joinCode.getValue()
        );

        // when
        roomCreateEventHandler.handle(event);
        roomCreateEventHandler.handle(event); // 중복 호출

        // then
        Room room = roomQueryService.getByJoinCode(joinCode);
        assertThat(room.getPlayers()).hasSize(1); // 호스트 한 명만 존재
    }
}
