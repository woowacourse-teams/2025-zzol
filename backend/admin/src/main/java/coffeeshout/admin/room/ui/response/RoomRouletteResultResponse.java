package coffeeshout.admin.room.ui.response;

import coffeeshout.admin.room.domain.RoomRouletteResult;
import java.time.LocalDateTime;

public record RoomRouletteResultResponse(
        Long winnerPlayerId, String winnerPlayerName, Integer winnerProbability, LocalDateTime createdAt) {

    public static RoomRouletteResultResponse from(RoomRouletteResult result) {
        return new RoomRouletteResultResponse(
                result.winnerPlayerId(), result.winnerPlayerName(),
                result.winnerProbability(), result.createdAt());
    }
}
