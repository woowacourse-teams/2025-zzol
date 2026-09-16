package coffeeshout.admin.room.ui.response;

import coffeeshout.admin.room.domain.RoomMiniGameResult;
import coffeeshout.minigame.domain.MiniGameType;
import java.time.LocalDateTime;

public record RoomMiniGameResultResponse(
        MiniGameType miniGameType,
        Long playerId,
        String playerName,
        Integer rank,
        Long score,
        LocalDateTime createdAt) {

    public static RoomMiniGameResultResponse from(RoomMiniGameResult result) {
        return new RoomMiniGameResultResponse(
                result.miniGameType(),
                result.playerId(),
                result.playerName(),
                result.rank(),
                result.score(),
                result.createdAt());
    }
}
