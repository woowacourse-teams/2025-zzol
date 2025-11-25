package coffeeshout.cardgame.domain.event.dto;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.CardGameTaskType;
import coffeeshout.room.domain.Room;

public record CardGameStateChangedEvent(
        Room room,
        CardGame cardGame,
        CardGameTaskType currentTask
) {
}
