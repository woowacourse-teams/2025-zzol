package coffeeshout.gamecommon;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * 방이 생성됐음을 알리는 공유 생명주기 이벤트(ADR-0025 결정 6). {@code :room}이 발행하고
 * {@code :room}(방 등록)과 {@code :game}(GameSession 사전 생성)이 함께 소비한다. 두 모듈이 모두
 * 의존하는 {@code :game-api}에 두어 어느 한쪽 모듈 내부에 종속되지 않게 한다.
 */
public record GameRoomCreatedEvent(
        String eventId,
        Instant timestamp,
        String hostName,
        String joinCode
) implements BaseEvent {

    public GameRoomCreatedEvent(String hostName, String joinCode) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                hostName,
                joinCode
        );
    }
}
