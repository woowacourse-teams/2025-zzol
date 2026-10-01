package coffeeshout.admin.room.domain;

import coffeeshout.minigame.domain.MiniGameType;
import java.time.LocalDateTime;

public record RoomMiniGameResult(
        MiniGameType miniGameType,
        Long playerId,
        String playerName,
        Integer rank,
        Long score,
        LocalDateTime createdAt) {}
