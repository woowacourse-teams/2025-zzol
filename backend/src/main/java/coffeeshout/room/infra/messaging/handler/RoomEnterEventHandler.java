package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.redis.EventHandler;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.MenuCommandService;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.infra.messaging.RoomEventWaitManager;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomEnterEventHandler implements EventHandler<RoomJoinEvent> {

    private final RoomCommandService roomCommandService;
    private final MenuCommandService menuCommandService;
    private final RoomEventWaitManager roomEventWaitManager;

    @Override
    public void handle(RoomJoinEvent event) {
        try {
            final SelectedMenuRequest selectedMenuRequest = event.selectedMenuRequest();

            final Menu menu = menuCommandService.convertMenu(
                    selectedMenuRequest.id(),
                    selectedMenuRequest.customName()
            );

            final Room room = roomCommandService.joinGuest(
                    new JoinCode(event.joinCode()),
                    new PlayerName(event.guestName()),
                    menu, selectedMenuRequest.temperature()
            );
            roomEventWaitManager.notifySuccess(event.eventId(), room);
        } catch (Exception e) {
            roomEventWaitManager.notifyFailure(event.eventId(), e);
            throw e;
        }
    }

    @Override
    public Class<RoomJoinEvent> eventType() {
        return RoomJoinEvent.class;
    }
}
