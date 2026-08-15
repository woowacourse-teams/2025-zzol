package coffeeshout.room.application.service;

import coffeeshout.friend.application.port.RoomMembershipQuery;
import coffeeshout.friend.domain.RoomMembership;
import coffeeshout.friend.domain.event.RoomPresenceChangedEvent;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 방의 회원 구성·입장 가능 여부가 바뀐 것을 감지해 친구 알림용 이벤트를 낸다.
 *
 * <p>{@code Room}의 상태 전이는 여러 메서드에 흩어져 있어 각각에 발행을 심으면 새 전이가 생길 때 빠뜨린다.
 * 대신 모든 변경이 반드시 통과하는 저장 지점 하나에서 직전 스냅샷과 비교한다 — 입장·퇴장·강퇴·게임 시작·정원참이
 * 전부 여기로 수렴한다. 바뀐 사용자에게만 발행하므로 준비 상태 토글 같은 무관한 저장은 아무것도 내보내지 않는다.
 *
 * <p>이 전제는 상태 전이 지점이 {@code RoomCommandService.save()}를 거칠 때만 성립한다. 인메모리 저장소는
 * 같은 객체 참조를 들고 있어 save 없이도 전이가 반영되므로, 새 전이를 추가할 때 save 호출을 빠뜨리면
 * 알림만 조용히 누락된다(#1266 리뷰에서 실제로 발견됐다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomPresencePublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final RoomMembershipQuery roomMembershipQuery;

    // ponytail: 방 삭제(onRoomDeleted)로만 정리된다. 삭제를 거치지 않고 사라지는 방이 생기면 TTL이 필요하다.
    private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();

    public void onRoomSaved(Room room) {
        final String joinCode = room.getJoinCode().getValue();
        final Snapshot current = Snapshot.of(room);

        // compute로 읽기-비교-쓰기를 방 단위로 원자화한다. 같은 방의 저장이 여러 스트림 스레드에서 겹쳐도
        // 스냅샷이 엇갈리지 않는다. 발행까지 이 안에서 끝내야 두 저장의 이벤트 순서가 뒤집히지 않는다.
        snapshots.compute(joinCode, (key, previous) -> {
            publishAll(diff(joinCode, previous, current));
            return current;
        });
    }

    public void onRoomDeleted(String joinCode) {
        final Snapshot previous = snapshots.remove(joinCode);
        if (previous == null) {
            return;
        }
        publishAll(previous.userIds().stream().map(this::leftEvent).toList());
    }

    private List<RoomPresenceChangedEvent> diff(String joinCode, Snapshot previous, Snapshot current) {
        final Set<Long> previousUserIds = previous == null ? Set.of() : previous.userIds();
        final RoomMembership membership = new RoomMembership(joinCode, current.joinable());
        final boolean joinableChanged = previous != null && previous.joinable() != current.joinable();

        return Stream.concat(
                        current.userIds().stream()
                                .filter(userId -> joinableChanged || !previousUserIds.contains(userId))
                                .map(userId -> new RoomPresenceChangedEvent(userId, membership)),
                        previousUserIds.stream()
                                .filter(userId -> !current.userIds().contains(userId))
                                .map(this::leftEvent))
                .toList();
    }

    /**
     * 방을 떠난 사용자의 이벤트. 무조건 {@link RoomMembership#NONE}을 보내면 방을 옮긴 사용자의 상태를 지운다 —
     * 퇴장 처리가 지연되는 동안(연결 해제 유예) 이미 다른 방에 들어갔을 수 있어, 현재 소속을 다시 확인한다.
     */
    private RoomPresenceChangedEvent leftEvent(Long userId) {
        final RoomMembership current =
                roomMembershipQuery.findByUserIds(List.of(userId)).getOrDefault(userId, RoomMembership.NONE);
        return new RoomPresenceChangedEvent(userId, current);
    }

    /**
     * 알림 실패가 방 처리 흐름을 끊지 못하게 한다. 발행은 동기라 수신 측(친구 조회·DB)의 예외가 그대로
     * {@code save()}·{@code delete()} 호출자까지 올라가면, 방은 이미 바뀐 채로 입장이 실패하거나
     * 방 삭제 후속 정리가 통째로 스킵된다. 한 사용자의 실패가 나머지 발행도 막지 않도록 개별 격리한다.
     */
    private void publishAll(List<RoomPresenceChangedEvent> events) {
        for (RoomPresenceChangedEvent event : events) {
            try {
                eventPublisher.publishEvent(event);
            } catch (Exception e) {
                log.warn("방 참여 상태 알림 발행 실패: userId={}, 원인={}", event.userId(), e.getMessage());
            }
        }
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
