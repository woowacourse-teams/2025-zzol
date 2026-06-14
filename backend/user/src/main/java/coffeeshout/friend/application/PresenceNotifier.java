package coffeeshout.friend.application;

import coffeeshout.friend.application.dto.PresencePayload;
import coffeeshout.friend.application.service.FriendshipService;
import coffeeshout.friend.domain.event.PresenceChangedEvent;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.UserPrincipal;
import coffeeshout.websocket.docs.WsQueue;
import coffeeshout.websocket.event.user.UserQueueSubscribedEvent;
import coffeeshout.websocket.ui.WebSocketResponse;
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

    @EventListener
    @Transactional(readOnly = true)
    @WsQueue(path = PRESENCE_QUEUE, payload = PresencePayload.class,
            description = "친구 접속 상태 변경 알림")
    public void onPresenceChanged(PresenceChangedEvent event) {
        final PresencePayload payload = new PresencePayload(event.userId(), event.online());

        friendshipService.findAcceptedFriendIds(event.userId()).forEach(friendId -> {
            try {
                messagingTemplate.convertAndSendToUser(
                        UserPrincipal.of(friendId), PRESENCE_QUEUE, WebSocketResponse.success(payload)
                );
                log.debug("Presence 알림: userId={}, online={}, friendId={}", event.userId(), event.online(), friendId);
            } catch (Exception e) {
                log.warn("Presence 알림 전송 실패: userId={}, 원인={}", event.userId(), e.getMessage());
            }
        });
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onPresenceQueueSubscribe(UserQueueSubscribedEvent event) {
        if (!PRESENCE_SUBSCRIBE_DEST.equals(event.destination())) {
            return;
        }

        final Long userId = event.userId();

        friendshipService.findAcceptedFriendIds(userId).stream()
                .filter(presenceTracker::isOnline)
                .forEach(friendId -> {
                    try {
                        messagingTemplate.convertAndSendToUser(
                                UserPrincipal.of(userId),
                                PRESENCE_QUEUE,
                                WebSocketResponse.success(new PresencePayload(friendId, true))
                        );
                        log.debug("초기 Presence 푸시: userId={}, onlineFriendId={}", userId, friendId);
                    } catch (Exception e) {
                        log.warn("초기 Presence 푸시 실패: userId={}, friendId={}, 원인={}", userId, friendId, e.getMessage());
                    }
                });
    }
}
