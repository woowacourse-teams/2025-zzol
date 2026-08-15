package coffeeshout.room.application.service;

import coffeeshout.friend.application.port.RoomMembership;
import coffeeshout.friend.domain.event.RoomPresenceChangedEvent;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 방의 회원 구성·입장 가능 여부가 바뀐 것을 감지해 친구 알림용 이벤트를 낸다.
 *
 * <p>{@code Room}의 상태 전이는 여러 메서드에 흩어져 있어 각각에 발행을 심으면 새 전이가 생길 때 빠뜨린다.
 * 대신 모든 변경이 반드시 통과하는 저장 지점 하나에서 직전 스냅샷과 비교한다 — 입장·퇴장·강퇴·게임 시작·정원참이
 * 전부 여기로 수렴한다. 바뀐 사용자에게만 발행하므로 준비 상태 토글 같은 무관한 저장은 아무것도 내보내지 않는다.
 */
@Component
@RequiredArgsConstructor
public class RoomPresencePublisher {

    private final ApplicationEventPublisher eventPublisher;

    // ponytail: 방 삭제(onRoomDeleted)로만 정리된다. 삭제를 거치지 않고 사라지는 방이 생기면 TTL이 필요하다.
    private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();

    public void onRoomSaved(Room room) {
        final String joinCode = room.getJoinCode().getValue();
        final Snapshot current = Snapshot.of(room);
        final List<RoomPresenceChangedEvent> events = new ArrayList<>();

        // compute로 읽기-비교-쓰기를 방 단위로 원자화한다. 같은 방의 저장이 여러 스트림 스레드에서 겹쳐도
        // 스냅샷이 엇갈리지 않는다.
        snapshots.compute(joinCode, (key, previous) -> {
            events.addAll(diff(joinCode, previous, current));
            return current;
        });

        events.forEach(eventPublisher::publishEvent);
    }

    public void onRoomDeleted(String joinCode) {
        final Snapshot previous = snapshots.remove(joinCode);
        if (previous == null) {
            return;
        }
        previous.userIds().forEach(this::publishLeft);
    }

    private List<RoomPresenceChangedEvent> diff(String joinCode, Snapshot previous, Snapshot current) {
        final Set<Long> previousUserIds = previous == null ? Set.of() : previous.userIds();
        final RoomMembership membership = new RoomMembership(joinCode, current.joinable());
        final List<RoomPresenceChangedEvent> events = new ArrayList<>();

        current.userIds().stream()
                .filter(userId -> !previousUserIds.contains(userId))
                .forEach(userId -> events.add(new RoomPresenceChangedEvent(userId, membership)));

        previousUserIds.stream()
                .filter(userId -> !current.userIds().contains(userId))
                .forEach(userId -> events.add(new RoomPresenceChangedEvent(userId, RoomMembership.NONE)));

        if (previous != null && previous.joinable() != current.joinable()) {
            current.userIds().stream()
                    .filter(previousUserIds::contains)
                    .forEach(userId -> events.add(new RoomPresenceChangedEvent(userId, membership)));
        }

        return events;
    }

    private void publishLeft(Long userId) {
        eventPublisher.publishEvent(new RoomPresenceChangedEvent(userId, RoomMembership.NONE));
    }

    private record Snapshot(Set<Long> userIds, boolean joinable) {

        private static Snapshot of(Room room) {
            final Set<Long> userIds = room.getPlayers().stream()
                    .map(Player::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
            return new Snapshot(userIds, room.isJoinable());
        }
    }
}
