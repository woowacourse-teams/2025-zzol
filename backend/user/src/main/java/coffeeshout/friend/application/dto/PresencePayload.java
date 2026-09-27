package coffeeshout.friend.application.dto;

import coffeeshout.friend.domain.RoomMembership;
import org.jspecify.annotations.Nullable;

/**
 * 친구 한 명의 현재 상태 전체 스냅샷. 접속 상태와 방 참여 상태는 서로 다른 시점에 바뀌지만,
 * 큐를 나누면 클라이언트가 두 부분 업데이트를 병합해야 하고 순서가 뒤집히면 상태가 어긋난다.
 * 그래서 어느 쪽이 바뀌든 항상 전체를 실어 보낸다.
 */
public record PresencePayload(
        Long userId, boolean online, @Nullable String joinCode, boolean joinable) {

    public static PresencePayload of(Long userId, boolean online, RoomMembership membership) {
        return new PresencePayload(userId, online, membership.joinCode(), membership.joinable());
    }
}
