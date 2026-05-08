package coffeeshout.friend.application;

import coffeeshout.friend.application.dto.PresencePayload;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.friend.domain.event.PresenceChangedEvent;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import coffeeshout.global.websocket.UserPrincipal;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceNotifier {

    private static final String PRESENCE_QUEUE = "/queue/friends/presence";

    private final FriendshipRepository friendshipRepository;
    private final LoggingSimpMessagingTemplate messagingTemplate;

    @EventListener
    @Transactional(readOnly = true)
    public void onPresenceChanged(PresenceChangedEvent event) {
        final PresencePayload payload = new PresencePayload(event.userId(), event.online());

        friendshipRepository.findAcceptedOf(event.userId()).forEach(friendship -> {
            try {
                final Long friendId = friendship.counterpartOf(event.userId());
                messagingTemplate.convertAndSendToUser(
                        UserPrincipal.of(friendId), PRESENCE_QUEUE, WebSocketResponse.success(payload)
                );
                log.debug("Presence 알림: userId={}, online={}, friendId={}", event.userId(), event.online(), friendId);
            } catch (Exception e) {
                log.warn("Presence 알림 전송 실패: userId={}, 원인={}", event.userId(), e.getMessage());
            }
        });
    }
}
