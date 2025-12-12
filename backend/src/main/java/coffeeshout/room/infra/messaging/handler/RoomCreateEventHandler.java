package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.redis.EventHandler;
import coffeeshout.room.application.DelayedRoomRemovalService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.MenuCommandService;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCreateEventHandler implements EventHandler<RoomCreateEvent> {

    private final DelayedRoomRemovalService delayedRoomRemovalService;
    private final RoomCommandService roomCommandService;
    private final MenuCommandService menuCommandService;

    @Override
    public void handle(RoomCreateEvent event) {
        final SelectedMenuRequest selectedMenuRequest = event.selectedMenuRequest();
        final Menu menu = menuCommandService.convertMenu(selectedMenuRequest.id(), selectedMenuRequest.customName());

        roomCommandService.saveIfAbsentRoom(
                new JoinCode(event.joinCode()),
                new PlayerName(event.hostName()),
                menu,
                selectedMenuRequest.temperature()
        );

        delayedRoomRemovalService.scheduleRemoveRoom(new JoinCode(event.joinCode()));
    }

    @Override
    public Class<RoomCreateEvent> eventType() {
        return RoomCreateEvent.class;
    }
}
