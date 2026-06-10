package coffeeshout.gamecommon;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * 방이 삭제됐음을 알리는 공유 생명주기 이벤트(ADR-0025 결정 6). {@code :room}이 발행하고
 * {@code :game}이 소비해 GameSession을 정리한다. 두 모듈이 모두 의존하는 {@code :game-api}에 둔다.
 */
public record GameRoomRemovedEvent(
        String eventId,
        Instant timestamp,
        String joinCode
) implements BaseEvent {

    public GameRoomRemovedEvent(String joinCode) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode
        );
    }
}
