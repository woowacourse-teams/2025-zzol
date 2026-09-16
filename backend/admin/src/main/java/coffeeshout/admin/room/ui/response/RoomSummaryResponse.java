package coffeeshout.admin.room.ui.response;

import coffeeshout.admin.room.domain.RoomSummary;
import coffeeshout.room.domain.RoomState;
import java.time.LocalDateTime;

public record RoomSummaryResponse(
        Long id,
        String joinCode,
        RoomState status,
        LocalDateTime createdAt,
        LocalDateTime finishedAt,
        long playerCount) {

    public static RoomSummaryResponse from(RoomSummary summary) {
        return new RoomSummaryResponse(
                summary.id(),
                summary.joinCode(),
                summary.status(),
                summary.createdAt(),
                summary.finishedAt(),
                summary.playerCount());
    }
}
