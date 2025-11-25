package coffeeshout.global.websocket.event;

import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.global.websocket.SubscriptionInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        final String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        final String destination = accessor.getDestination();
        final String subscriptionId = accessor.getSubscriptionId();

        // 구독 정보 추가
        subscriptionInfoService.addSubscription(sessionId, destination, subscriptionId);

        // INFO 레벨에서도 상세 구독 정보 로깅 (구독자 수 포함)
        subscriptionInfoService.logSubscriptionInfo(destination);
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
