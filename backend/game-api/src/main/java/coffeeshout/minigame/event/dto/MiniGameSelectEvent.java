package coffeeshout.minigame.event.dto;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.minigame.domain.MiniGameType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MiniGameSelectEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String hostName,
        List<MiniGameType> miniGameTypes,
        // 비동기 Consumer 처리 실패 시 에러를 되돌려 보낼 WebSocket Principal 이름(요청 클라이언트 식별). 없으면 null.
        String principalName
) implements BaseEvent {

    public MiniGameSelectEvent(String joinCode, String hostName, List<MiniGameType> miniGameTypes) {
        this(joinCode, hostName, miniGameTypes, null);
    }

    public MiniGameSelectEvent(String joinCode, String hostName, List<MiniGameType> miniGameTypes, String principalName) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                hostName,
                miniGameTypes,
                principalName
        );
    }
}
