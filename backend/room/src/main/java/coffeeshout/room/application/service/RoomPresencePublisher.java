package coffeeshout.room.application.service;

import coffeeshout.friend.application.port.RoomMembershipQuery;
import coffeeshout.friend.domain.RoomMembership;
import coffeeshout.friend.domain.event.RoomPresenceChangedEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 방의 회원 구성·입장 가능 여부가 바뀐 것을 감지해 친구 알림용 이벤트를 낸다.
 *
 * <p>{@code Room}의 상태 전이는 여러 메서드에 흩어져 있어 각각에 발행을 심으면 새 전이가 생길 때 빠뜨린다.
 * 대신 모든 변경이 반드시 통과하는 저장 지점 하나에서 변이 직전·직후 {@link RoomPresence}를 비교한다 —
 * 입장·퇴장·강퇴·게임 시작·정원참이 전부 여기로 수렴한다. 바뀐 사용자에게만 발행하므로 준비 상태 토글 같은
 * 무관한 저장은 아무것도 내보내지 않는다.
 *
 * <p>직전 상태는 {@code RoomCommandService}가 변이 직전에 떠서 넘긴다. 이 클래스는 상태를 갖지 않는다 —
 * 방 밖에 사는 사본이 없으니 정리·수명 관리도, 발행 구간 전체를 잠글 이유도 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomPresencePublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final RoomMembershipQuery roomMembershipQuery;

    public void publish(RoomPresence before, RoomPresence after) {
        publishAll(stayedEvents(before, after));
        publishAll(leftEvents(before, after));
    }

    /**
     * 방에 남아 있는 사용자 중 <b>새로 들어왔거나</b> 입장 가능 여부가 바뀐 사람. 둘 다 그대로면 비어 있다.
     */
    private List<RoomPresenceChangedEvent> stayedEvents(RoomPresence before, RoomPresence after) {
        final RoomMembership membership = new RoomMembership(after.joinCode(), after.joinable());
        final boolean joinableChanged = before.joinable() != after.joinable();

        return after.userIds().stream()
                .filter(userId -> joinableChanged || !before.userIds().contains(userId))
                .map(userId -> new RoomPresenceChangedEvent(userId, membership))
                .toList();
    }

    /**
     * 방을 떠난 사용자의 이벤트. 무조건 {@link RoomMembership#NONE}을 보내면 방을 옮긴 사용자의 상태를 지운다 —
     * 퇴장 처리가 지연되는 동안(연결 해제 유예) 이미 다른 방에 들어갔을 수 있어, 현재 소속을 다시 확인한다.
     * 조회는 인원수만큼이 아니라 <b>한 번에</b> 한다({@code findByUserIds}가 Collection을 받는 이유다).
     *
     * <p>조회 실패는 여기서 막는다. 이 예외가 그대로 올라가면 {@code save()}·{@code delete()} 호출자까지
     * 도달해 방은 이미 바뀐 채로 퇴장·삭제 처리가 통째로 스킵된다. 알림 한 번을 잃는 편이 낫다.
     */
    private List<RoomPresenceChangedEvent> leftEvents(RoomPresence before, RoomPresence after) {
        final Set<Long> leftUserIds = before.userIds().stream()
                .filter(userId -> !after.userIds().contains(userId))
                .collect(Collectors.toUnmodifiableSet());

        if (leftUserIds.isEmpty()) {
            return List.of();
        }

        try {
            final Map<Long, RoomMembership> memberships = roomMembershipQuery.findByUserIds(leftUserIds);
            return leftUserIds.stream()
                    .map(userId ->
                            new RoomPresenceChangedEvent(userId, memberships.getOrDefault(userId, RoomMembership.NONE)))
                    .toList();
        } catch (Exception e) {
            log.warn("퇴장자 현재 소속 조회 실패 - 퇴장 알림을 건너뛴다: userIds={}, 원인={}", leftUserIds, e.getMessage());
            return List.of();
        }
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
}
