package coffeeshout.friend.application;

import coffeeshout.friend.application.dto.PresencePayload;
import coffeeshout.friend.application.port.RoomMembershipQuery;
import coffeeshout.friend.application.service.FriendshipService;
import coffeeshout.friend.domain.RoomMembership;
import coffeeshout.friend.domain.event.PresenceChangedEvent;
import coffeeshout.friend.domain.event.RoomPresenceChangedEvent;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.UserPrincipal;
import coffeeshout.websocket.docs.WsQueue;
import coffeeshout.websocket.event.user.UserQueueSubscribedEvent;
import coffeeshout.websocket.ui.WebSocketResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceNotifier {

    private static final String PRESENCE_QUEUE = "/queue/friends/presence";
    private static final String PRESENCE_SUBSCRIBE_DEST = "/user/queue/friends/presence";

    private final FriendshipService friendshipService;
    private final LoggingSimpMessagingTemplate messagingTemplate;
    private final PresenceTracker presenceTracker;
    private final RoomMembershipQuery roomMembershipQuery;

    @EventListener
    @Transactional(readOnly = true)
    @WsQueue(path = PRESENCE_QUEUE, payload = PresencePayload.class, description = "친구 접속 상태 변경 알림")
    public void onPresenceChanged(PresenceChangedEvent event) {
        final RoomMembership membership = findMembership(event.userId());
        broadcastToFriends(PresencePayload.of(event.userId(), event.online(), membership));
    }

    /**
     * 방 참여 상태만 바뀐 경우. 접속 상태는 이 이벤트가 모르므로 {@link PresenceTracker}에서 읽어 스냅샷을 완성한다.
     */
    @EventListener
    @Transactional(readOnly = true)
    @WsQueue(path = PRESENCE_QUEUE, payload = PresencePayload.class, description = "친구 방 참여 상태 변경 알림")
    public void onRoomPresenceChanged(RoomPresenceChangedEvent event) {
        final boolean online = presenceTracker.isOnline(event.userId());
        broadcastToFriends(PresencePayload.of(event.userId(), online, event.membership()));
    }

    @EventListener
    @Transactional(readOnly = true)
    @WsQueue(path = PRESENCE_QUEUE, payload = PresencePayload.class, description = "큐 구독 시 온라인 친구 상태 일괄 푸시")
    public void onPresenceQueueSubscribe(UserQueueSubscribedEvent event) {
        if (!PRESENCE_SUBSCRIBE_DEST.equals(event.destination())) {
            return;
        }

        final Long userId = event.userId();
        final List<Long> onlineFriendIds = friendshipService.findAcceptedFriendIds(userId).stream()
                .filter(presenceTracker::isOnline)
                .toList();
        final Map<Long, RoomMembership> membershipByFriendId = roomMembershipQuery.findByUserIds(onlineFriendIds);

        onlineFriendIds.forEach(friendId -> {
            final RoomMembership membership = membershipByFriendId.getOrDefault(friendId, RoomMembership.NONE);
            sendTo(userId, PresencePayload.of(friendId, true, membership), "초기 Presence 푸시");
        });
    }

    private void broadcastToFriends(PresencePayload payload) {
        friendshipService
                .findAcceptedFriendIds(payload.userId())
                .forEach(friendId -> sendTo(friendId, payload, "Presence 알림"));
    }

    private RoomMembership findMembership(Long userId) {
        return roomMembershipQuery.findByUserIds(List.of(userId)).getOrDefault(userId, RoomMembership.NONE);
    }

    private void sendTo(Long addresseeId, PresencePayload payload, String logLabel) {
        try {
            messagingTemplate.convertAndSendToUser(
                    UserPrincipal.of(addresseeId), PRESENCE_QUEUE, WebSocketResponse.success(payload));
            log.debug(
                    "{}: userId={}, online={}, joinCode={}, 수신자={}",
                    logLabel,
                    payload.userId(),
                    payload.online(),
                    payload.joinCode(),
                    addresseeId);
        } catch (Exception e) {
            log.warn("{} 전송 실패: userId={}, 수신자={}, 원인={}", logLabel, payload.userId(), addresseeId, e.getMessage());
        }
    }
}
