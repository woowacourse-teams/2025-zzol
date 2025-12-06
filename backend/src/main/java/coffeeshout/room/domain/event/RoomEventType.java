package coffeeshout.room.domain.event;

import lombok.Getter;

@Getter
public enum RoomEventType {
    ROOM_CREATE(RoomCreateEvent.class),
    ROOM_JOIN(RoomJoinEvent.class),
    PLAYER_READY(PlayerReadyEvent.class),
    PLAYER_LIST_UPDATE(PlayerListUpdateEvent.class),
    PLAYER_KICK(PlayerKickEvent.class),
    MINI_GAME_SELECT(MiniGameSelectEvent.class),
    ROULETTE_SHOW(RouletteShowEvent.class),
    ROULETTE_SPIN(RouletteSpinEvent.class),
    QR_CODE_COMPLETE(QrCodeStatusEvent.class);

    private final Class<? extends RoomBaseEvent> eventClass;

    RoomEventType(Class<? extends RoomBaseEvent> eventClass) {
        this.eventClass = eventClass;
    }

}
