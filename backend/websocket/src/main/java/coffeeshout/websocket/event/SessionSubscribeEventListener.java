package coffeeshout.websocket.event;

import coffeeshout.websocket.StompSessionManager;
import coffeeshout.websocket.SubscriptionInfoService;
import coffeeshout.websocket.UserPrincipal;
import coffeeshout.websocket.event.user.UserQueueSubscribedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionSubscribeEventListener {

    private final StompSessionManager sessionManager;
    private final SubscriptionInfoService subscriptionInfoService;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        final String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        final String destination = accessor.getDestination();
        final String subscriptionId = accessor.getSubscriptionId();

        subscriptionInfoService.addSubscription(sessionId, destination, subscriptionId);
        subscriptionInfoService.logSubscriptionInfo(destination);

        final Long userId = UserPrincipal.extractUserId(event.getUser());
        if (userId != null && destination != null) {
            eventPublisher.publishEvent(new UserQueueSubscribedEvent(userId, destination));
        }
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        final String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        final String subscriptionId = accessor.getSubscriptionId();

        // 구독 정보 제거 - subscriptionId로 정확한 destination 찾아서 제거
        subscriptionInfoService.removeSubscription(sessionId, subscriptionId);
    }
}
