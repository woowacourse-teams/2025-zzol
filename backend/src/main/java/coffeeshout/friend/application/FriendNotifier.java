package coffeeshout.friend.application;

import coffeeshout.friend.application.dto.FriendRemovedPayload;
import coffeeshout.friend.application.dto.FriendRequestPayload;
import coffeeshout.friend.application.dto.FriendResponsePayload;
import coffeeshout.friend.application.dto.RoomInvitationPayload;
import coffeeshout.friend.domain.event.FriendRemovedEvent;
import coffeeshout.friend.domain.event.FriendRequestAcceptedEvent;
import coffeeshout.friend.domain.event.FriendRequestCreatedEvent;
import coffeeshout.friend.domain.event.FriendRequestRejectedEvent;
import coffeeshout.friend.domain.event.RoomInvitationSentEvent;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.UserPrincipal;
import coffeeshout.websocket.ui.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendNotifier {

    private static final String FRIEND_REQUESTS_QUEUE = "/queue/friends/requests";
    private static final String FRIEND_RESPONSES_QUEUE = "/queue/friends/responses";
    private static final String FRIEND_REMOVED_QUEUE = "/queue/friends/removed";
    private static final String ROOM_INVITATIONS_QUEUE = "/queue/rooms/invitations";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestCreated(FriendRequestCreatedEvent event) {
        final FriendRequestPayload payload = new FriendRequestPayload(
                event.requestId(),
                event.requesterId(),
                event.requesterUserCode(),
                event.requesterNickname(),
                event.timestamp()
        );
        sendToUser(event.addresseeId(), FRIEND_REQUESTS_QUEUE, payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        final FriendResponsePayload toRequester = new FriendResponsePayload(
                event.requestId(), true,
                event.addresseeId(), event.addresseeUserCode(), event.addresseeNickname()
        );
        sendToUser(event.requesterId(), FRIEND_RESPONSES_QUEUE, toRequester);

        final FriendResponsePayload toAddressee = new FriendResponsePayload(
                event.requestId(), true,
                event.requesterId(), event.requesterUserCode(), event.requesterNickname()
        );
        sendToUser(event.addresseeId(), FRIEND_RESPONSES_QUEUE, toAddressee);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestRejected(FriendRequestRejectedEvent event) {
        final FriendResponsePayload payload = new FriendResponsePayload(
                event.requestId(), false,
                event.addresseeId(), event.addresseeUserCode(), event.addresseeNickname()
        );
        sendToUser(event.requesterId(), FRIEND_RESPONSES_QUEUE, payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRemoved(FriendRemovedEvent event) {
        sendToUser(event.targetUserId(), FRIEND_REMOVED_QUEUE, new FriendRemovedPayload(event.removedByUserId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomInvitationSent(RoomInvitationSentEvent event) {
        sendToUser(event.targetUserId(), ROOM_INVITATIONS_QUEUE, RoomInvitationPayload.from(event));
    }

    private void sendToUser(Long userId, String destination, Object payload) {
        try {
            final String principal = UserPrincipal.of(userId);
            messagingTemplate.convertAndSendToUser(principal, destination, WebSocketResponse.success(payload));
            log.debug("STOMP 개인 알림 전송: userId={}, destination={}", userId, destination);
        } catch (Exception e) {
            log.warn("STOMP 개인 알림 전송 실패: userId={}, destination={}, 원인={}", userId, destination, e.getMessage());
        }
    }
}
