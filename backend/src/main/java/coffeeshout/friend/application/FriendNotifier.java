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
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.global.websocket.UserPrincipal;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import java.time.Instant;
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
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestCreated(FriendRequestCreatedEvent event) {
        userRepository.findById(event.requesterId()).ifPresentOrElse(
                requester -> {
                    final FriendRequestPayload payload = new FriendRequestPayload(
                            event.requestId(),
                            requester.getId(),
                            requester.getUserCode().value(),
                            requester.getNickname().value(),
                            Instant.now()
                    );
                    sendToUser(event.addresseeId(), FRIEND_REQUESTS_QUEUE, payload);
                },
                () -> log.warn("친구 요청 알림 실패 — 요청자 없음: requesterId={}", event.requesterId())
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        userRepository.findById(event.addresseeId()).ifPresent(addressee ->
                sendFriendResponse(event.requestId(), event.requesterId(), addressee, true)
        );
        userRepository.findById(event.requesterId()).ifPresent(requester ->
                sendFriendResponse(event.requestId(), event.addresseeId(), requester, true)
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestRejected(FriendRequestRejectedEvent event) {
        userRepository.findById(event.addresseeId()).ifPresent(addressee ->
                sendFriendResponse(event.requestId(), event.requesterId(), addressee, false)
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRemoved(FriendRemovedEvent event) {
        sendToUser(event.targetUserId(), FRIEND_REMOVED_QUEUE, new FriendRemovedPayload(event.removedByUserId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomInvitationSent(RoomInvitationSentEvent event) {
        sendToUser(event.targetUserId(), ROOM_INVITATIONS_QUEUE, RoomInvitationPayload.from(event));
    }

    private void sendFriendResponse(Long requestId, Long targetUserId, User counterpart, boolean accepted) {
        final FriendResponsePayload payload = new FriendResponsePayload(
                requestId, accepted,
                counterpart.getId(),
                counterpart.getUserCode().value(),
                counterpart.getNickname().value()
        );
        sendToUser(targetUserId, FRIEND_RESPONSES_QUEUE, payload);
    }

    private void sendToUser(Long userId, String destination, Object payload) {
        final String principal = UserPrincipal.of(userId);
        messagingTemplate.convertAndSendToUser(principal, destination, WebSocketResponse.success(payload));
        log.debug("STOMP 개인 알림 전송: userId={}, destination={}", userId, destination);
    }
}
