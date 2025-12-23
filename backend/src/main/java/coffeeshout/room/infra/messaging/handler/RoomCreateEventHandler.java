package coffeeshout.room.infra.messaging.handler;

import coffeeshout.room.application.DelayedRoomRemovalService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.MenuCommandService;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomCreateEventHandler implements Consumer<RoomCreateEvent> {

    private final RoomCommandService roomCommandService;
    private final MenuCommandService menuCommandService;
    private final DelayedRoomRemovalService delayedRoomRemovalService;

    @Override
    public void accept(RoomCreateEvent event) {
        final SelectedMenuRequest selectedMenuRequest = event.selectedMenuRequest();
        final Menu menu = menuCommandService.convertMenu(
                selectedMenuRequest.id(),
                selectedMenuRequest.customName()
        );

        roomCommandService.saveIfAbsentRoom(
                new JoinCode(event.joinCode()),
                new PlayerName(event.hostName()),
                menu,
                selectedMenuRequest.temperature()
        );

        delayedRoomRemovalService.scheduleRemoveRoom(new JoinCode(event.joinCode()));
    }
}
