package coffeeshout.friend.domain.event;

import coffeeshout.friend.domain.RoomMembership;
import java.time.Instant;
import java.util.UUID;

/**
 * 사용자의 방 참여 상태가 바뀐 것을 알린다. 방에서 나갔거나 방이 사라졌으면 {@code membership}이
 * {@link RoomMembership#NONE}이다.
 *
 * <p>접속 상태({@link PresenceChangedEvent})와 별개의 트리거지만 같은 큐로 나간다 — 클라이언트에는
 * 언제나 전체 스냅샷을 보낸다.
 */
public record RoomPresenceChangedEvent(String eventId, Instant timestamp, Long userId, RoomMembership membership) {

    public RoomPresenceChangedEvent(Long userId, RoomMembership membership) {
        this(UUID.randomUUID().toString(), Instant.now(), userId, membership);
    }
}
