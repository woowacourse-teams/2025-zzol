package coffeeshout.room.application.service;

import coffeeshout.room.domain.Room;

public record RoomCreateResult(Room room, String roomSessionToken) {
}
